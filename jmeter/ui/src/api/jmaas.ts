import { z } from 'zod'
import axios from 'axios';
import type { AxiosProgressEvent } from 'axios'
import { toString, split, isEqual } from 'lodash'
import { TestSchema } from '../stores/test.ts'
import type { Test } from '../stores/test.ts'

const BASE = '/jmeter/test';

const KeyValueParamsEnum = [
    "query",
    "headers",
] as const;

export const KeyValueParamsSchema = z.enum(KeyValueParamsEnum);

type KeyValueParams = z.infer<typeof KeyValueParamsSchema>;

const getKeyValueParam = (
    test: Test,
    param: KeyValueParams,
): string => {
    let value = "";
    test[param]
        .forEach((p) => {
            value += `${p.key}=${p.value};`;
        });
    return value;
};

const MultiValueParamsEnum = [
    "qps",
    "concurrency",
] as const;

export const MultiValueParamsSchema = z.enum(MultiValueParamsEnum);

type MultiValueParams = z.infer<typeof MultiValueParamsSchema>;

const getMultiValueParam = (
    test: Test,
    param: MultiValueParams,
): string => {
    let value = "";
    test[param]
        .forEach((v) => {
            value += `${v};`;
        });
    return value;
};

const headersPrefix = "x-jmaas-test";

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

        headers[`${headersPrefix}-mode`] = test.mode;
        headers[`${headersPrefix}-script`] = test.script;
        headers[`${headersPrefix}-method`] = test.method;
        headers[`${headersPrefix}-host`] = test.host;
        headers[`${headersPrefix}-port`] = toString(test.port);
        headers[`${headersPrefix}-path`] = test.path;
        headers[`${headersPrefix}-async`] = toString(test.async || false);
        headers[`${headersPrefix}-duration`] = toString(test.duration);
        headers[`${headersPrefix}-min-latency`] = toString(test.minLatency);
        headers[`${headersPrefix}-max-latency`] = toString(test.maxLatency);
        headers[`${headersPrefix}-query`] = getKeyValueParam(test, KeyValueParamsSchema.Values.query);
        headers[`${headersPrefix}-headers`] = getKeyValueParam(test, KeyValueParamsSchema.Values.headers);

        if (isEqual(mode, "qps")) {
            headers[`${headersPrefix}-qps`] = getMultiValueParam(test, MultiValueParamsSchema.Values.qps);
        } else {
            headers[`${headersPrefix}-concurrency`] = getMultiValueParam(test, MultiValueParamsSchema.Values.concurrency);
        }

        return axios.request({
            url: `${BASE}/run`,
            method,
            headers,
            onDownloadProgress(progressEvent: AxiosProgressEvent) {
                onDownloadProgress(test, progressEvent, handler);
            },
        });
    },
};