import { z } from 'zod'
import axios from 'axios';
import { toString, split, isEqual, toNumber } from 'lodash'
import { TestSchema } from '../stores/test.ts'
import {
    KeyValueParamsSchema,
    MultiValueParamsSchema,
} from '../types/catalogs.ts'
import type { AxiosProgressEvent } from 'axios'
import type {
    KeyValueParamsType,
    MultiValueParamType,
    MultiValueParamsType,
    QPS, Concurrency,
} from '../types/catalogs.ts'
import type { Test } from '../stores/test.ts'

const BASE = '/jmeter/test';

const getKeyValueParam = (
    test: Test,
    param: KeyValueParamsType,
): string => {
    let value = "";
    test[param]
        .forEach((p) => {
            value += `${p.key}=${p.value};`;
        });
    return value;
};

const getMultiValueParam = (
    test: Test,
    param: MultiValueParamsType,
): Array<MultiValueParamType> => {
    const data = test[param];
    const values = data.values();
    return [...values];
};

const getQPS = (
    test: Test,
): [string, number] => {
    let value = "";
    const qps = getMultiValueParam(
        test, MultiValueParamsSchema.Values.qps,
    ) as Array<QPS>;
    let duration = 0;
    qps.forEach((p) => {
        duration += toNumber(p.duration);
        value += `${p.startQPS},${p.endQPS},${p.duration};`;
    });
    return [value, duration];
};

const getConcurrency = (
    test: Test,
): [string, number] => {
    let value = "";
    const concurrency = getMultiValueParam(
        test, MultiValueParamsSchema.Values.concurrency,
    ) as Array<Concurrency>;
    let duration = 0;
    concurrency.forEach((p) => {
        duration += toNumber(p.rampupTime) + toNumber(p.duration) + toNumber(p.shutdownTime);
        value += `${p.threadCount},${p.initialDelay},${p.rampupTime},${p.duration},${p.shutdownTime};`;
    });
    return [value, duration];
};

export const headersPrefix = "x-jmaas-test";

const HeadersSchema = z.record(z.string(), z.string());

export type Headers = z.infer<typeof HeadersSchema>;

const TestStreamEventSchema = z.object({
    status: z.number(),
    statusText: z.string(),
    headers: HeadersSchema,
    response: z.any(),
});

export type TestStreamEvent = z.infer<typeof TestStreamEventSchema>;

const TestStreamHandlerSchema =
    z.function()
        .args(
            TestSchema,
            TestStreamEventSchema
        )
        .returns(z.void());

export type TestStreamHandler = z.infer<typeof TestStreamHandlerSchema>;

const parseHeaders = (request: XMLHttpRequest): Headers => {
    const rawHeaders = request.getAllResponseHeaders();
    const headersList = split(rawHeaders, /[\r\n]+/);
    const headers: Headers = {};

    headersList.forEach((line) => {
        const parts = split(line, /:\s?/, 2);
        const header = parts[0];
        const value = parts[1];
        headers[header] = value;
    });

    return headers;
};

const onDownloadProgress = (
    test: Test,
    progressEvent: AxiosProgressEvent,
    handler: TestStreamHandler,
) => {
    const event = progressEvent.event as ProgressEvent;
    const request = event.currentTarget as XMLHttpRequest;
    handler(test, {
        status: request.status,
        statusText: request.statusText,
        headers: parseHeaders(request),
        response: request.response,
    });
}

export const headerName = (
    name: string,
) => {
    return `${headersPrefix}-${name}`
};

export const X_JMAAS_TEST_ID = headerName('id');

export const JMAAS_HEADERS = {
    ID: X_JMAAS_TEST_ID,
    TEST_ID: X_JMAAS_TEST_ID,
    TRACE_ID: headerName('trace-id'),
    INSTANCE_ID: headerName('instance-id'),
    MODE: headerName('mode'),
    SCRIPT: headerName('script'),
    METHOD: headerName('method'),
    HOST: headerName('host'),
    PORT: headerName('port'),
    PATH: headerName('path'),
    ASYNC: headerName('async'),
    DURATION: headerName('duration'),
    MIN_LATENCY: headerName('min-latency'),
    MAX_LATENCY: headerName('max-latency'),
    QUERY: headerName('query'),
    HEADERS: headerName('headers'),
    QPS: headerName('qps'),
    CONCURRENCY: headerName('concurrency'),
};

export default {
    getCatalog: (catalog: string = "default") => {
        return axios.get(`${BASE}/catalog/${catalog}`);
    },

    runTest: (
        test: Test,
        handler: TestStreamHandler,
    ) => {
        const mode = test.mode;
        const method = test.method;
        const headers: Headers = {};

        headers[JMAAS_HEADERS.MODE] = test.mode;
        headers[JMAAS_HEADERS.SCRIPT] = test.script;
        headers[JMAAS_HEADERS.METHOD] = test.method;
        headers[JMAAS_HEADERS.HOST] = test.host;
        headers[JMAAS_HEADERS.PORT] = toString(test.port);
        headers[JMAAS_HEADERS.PATH] = test.path;
        headers[JMAAS_HEADERS.ASYNC] = toString(test.async || false);
        headers[JMAAS_HEADERS.DURATION] = toString(test.duration);
        headers[JMAAS_HEADERS.MIN_LATENCY] = toString(test.minLatency);
        headers[JMAAS_HEADERS.MAX_LATENCY] = toString(test.maxLatency);
        headers[JMAAS_HEADERS.QUERY] = getKeyValueParam(test, KeyValueParamsSchema.Values.query);
        headers[JMAAS_HEADERS.HEADERS] = getKeyValueParam(test, KeyValueParamsSchema.Values.headers);

        let trafficShape: string;
        let duration: number;
        if (isEqual(mode, MultiValueParamsSchema.Enum.qps)) {
            [trafficShape, duration] = getQPS(test);
            headers[JMAAS_HEADERS.QPS] = trafficShape;
        } else {
            [trafficShape, duration] = getConcurrency(test);
            headers[JMAAS_HEADERS.CONCURRENCY] = trafficShape;
        }

        if ( toNumber(test.duration) != duration ) {
            throw new Error(`invalid duration: ${test.duration} != ${duration}`);
        }

        return axios.request({
            url: `${BASE}/run`,
            method,
            headers,
            onDownloadProgress(
                progressEvent: AxiosProgressEvent,
            ) {
                onDownloadProgress(
                    test, progressEvent, handler
                );
            },
        });
    },
};