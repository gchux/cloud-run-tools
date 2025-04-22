import { z } from 'zod'
import axios from 'axios';
import { toString, split, isEqual, toNumber, isUndefined, isNull, partial, set, omit } from 'lodash'
import {
    KeyValueParamsSchema,
    MethodEnumSchema,
    ModeEnumSchema,
    MultiValueParamsSchema,
    ProtoEnumSchema,
} from '../types/catalogs.ts'
import type { AxiosHeaders, AxiosProgressEvent, AxiosRequestConfig, AxiosResponse } from 'axios'
import type {
    KeyValueParamsType,
    MultiValueParamType,
    MultiValueParamsType,
    QPS, Concurrency,
} from '../types/catalogs.ts'
import { DurationSchema, MinMaxLatencySchema, PortSchema, type Test } from '../stores/test.ts'

const BASE = '/jmeter/test';

const NonEmptyString = z.string().nonempty().trim();

const ParamsOrHeadersSchema = z.record(NonEmptyString, z.any());

const TestDetailsSchema = z.object({
    id: z.string().uuid(),
    name: NonEmptyString,
    trace_id: NonEmptyString,
    instance_id: NonEmptyString,
    script: NonEmptyString,
    mode: ModeEnumSchema,
    host: NonEmptyString,
    port: PortSchema,
    method: MethodEnumSchema,
    proto: ProtoEnumSchema,
    path: NonEmptyString,
    payload: z.optional(NonEmptyString),
    params: ParamsOrHeadersSchema,
    headers: ParamsOrHeadersSchema,
    duration: DurationSchema,
    min_latency: MinMaxLatencySchema,
    max_latency: MinMaxLatencySchema,
});

export type TestDetails = z.infer<typeof TestDetailsSchema>;

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
            TestStreamEventSchema,
        )
        .returns(z.void());

const TestStatusHandlerSchema =
    z.function()
        .args(
            TestDetailsSchema,
        )
        .returns(z.void());

export type TestStreamHandler = z.infer<typeof TestStreamHandlerSchema>;

export type TestStatusHandler = z.infer<typeof TestStatusHandlerSchema>;

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

export const getHeaders = (
    test: Test,
) => {
    return getKeyValueParam(
        test, KeyValueParamsSchema.Values.headers
    );
};

export const getQuery = (
    test: Test,
) => {
    return getKeyValueParam(
        test, KeyValueParamsSchema.Values.query
    );
};

const getMultiValueParam = (
    test: Test,
    param: MultiValueParamsType,
): Array<MultiValueParamType> => {
    const data = test[param];
    const values = data.values();
    return [...values];
};

export const getQPS = (
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

export const getConcurrency = (
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

const parseHeaders = (request: XMLHttpRequest): Headers => {
    const rawHeaders = request.getAllResponseHeaders();
    const headersList = split(rawHeaders, /[\r\n]+/);
    const headers: Headers = {};

    headersList.forEach((line) => {
        const parts = split(line, /:\s?/, 2);
        set(headers, parts[0], parts[1]);
    });

    return headers;
};

const setHaeders = (
    event: TestStreamEvent,
    headers: AxiosHeaders,
): TestStreamEvent => {
    for (const [headerName, value] of headers) {
        if (!isUndefined(value) && !isNull(value)) {
            set(event.headers, headerName, value.toString());
        }
    }
    return event;
};

const onDownloadProgress = (
    handler: TestStreamHandler,
    progressEvent: AxiosProgressEvent,
) => {
    const event = progressEvent.event as ProgressEvent;
    const request = event.currentTarget as XMLHttpRequest;
    handler({
        status: request.status,
        statusText: request.statusText,
        headers: parseHeaders(request),
        response: request.response,
    });
}

const onResponse = (
    handler: TestStreamHandler,
    response: AxiosResponse,
) => {
    const event: TestStreamEvent = {
        status: response.status,
        statusText: response.statusText,
        headers: {},
        response: "",
    };
    const headers = response.headers as AxiosHeaders;
    handler(setHaeders(event, headers));
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
    PARAMS: headerName('params'),
    HEADERS: headerName('headers'),
    QPS: headerName('qps'),
    CONCURRENCY: headerName('concurrency'),
};

export default {
    getCatalog: async (catalog: string = "default") => {
        return axios.get(`${BASE}/catalog/${catalog}`);
    },

    getTestByID: async (
        testID: string,
        callback?: TestStatusHandler,
    ) => {
        return axios
            .get(`${BASE}/status/${testID}`)
            .then((response) => {
                if (isUndefined(callback)) {
                    return;
                }

                const payload: Record<string, any> = response.data

                const { data }: Record<string, any> = payload
                if (isUndefined(data)) {
                    return;
                }

                const testDetails = TestDetailsSchema.safeParse(data);
                if (testDetails.success) {
                    callback(testDetails.data);
                } else {
                    console.error(testDetails.error);
                }
            });
    },

    streamTest: (
        testID: string,
        handler: TestStreamHandler
    ) => {
        return axios.request({
            url: `${BASE}/stream/${testID}`,
            method: "GET",
            headers: {
                [JMAAS_HEADERS.TEST_ID]: testID,
            },
            onDownloadProgress: partial(
                onDownloadProgress, handler
            ),
        });
    },

    runTest: (
        test: Test,
        handler: TestStreamHandler,
    ): Promise<AxiosResponse> => {
        const mode = test.mode;
        const method = test.method;
        const testDuration = toNumber(test.duration);
        const isAsync = test.async;
        const headers: Headers = {};

        set(headers, JMAAS_HEADERS.TEST_ID, test.id);
        set(headers, JMAAS_HEADERS.MODE, test.mode);
        set(headers, JMAAS_HEADERS.SCRIPT, test.script);
        set(headers, JMAAS_HEADERS.METHOD, test.method);
        set(headers, JMAAS_HEADERS.HOST, test.host);
        set(headers, JMAAS_HEADERS.PORT, toString(test.port));
        set(headers, JMAAS_HEADERS.PATH, test.path);
        set(headers, JMAAS_HEADERS.ASYNC, toString(isAsync));
        set(headers, JMAAS_HEADERS.DURATION, toString(testDuration));
        set(headers, JMAAS_HEADERS.MIN_LATENCY, toString(test.minLatency));
        set(headers, JMAAS_HEADERS.MAX_LATENCY, toString(test.maxLatency));
        set(headers, JMAAS_HEADERS.PARAMS, getQuery(test));
        set(headers, JMAAS_HEADERS.HEADERS, getHeaders(test));

        let trafficShape: string;
        let duration: number;
        if (isEqual(mode, MultiValueParamsSchema.Enum.qps)) {
            [trafficShape, duration] = getQPS(test);
            set(headers, JMAAS_HEADERS.QPS, trafficShape);
        } else {
            [trafficShape, duration] = getConcurrency(test);
            set(headers, JMAAS_HEADERS.CONCURRENCY, trafficShape);
        }

        if (testDuration != duration) {
            throw new Error(`invalid duration: ${test.duration} != ${duration}`);
        }

        if ((testDuration > 3600) && !isAsync) {
            throw new Error(`invalid duration: ${testDuration} is greater than 1 hour; consider using 'async'`);
        }

        const request: AxiosRequestConfig = {
            url: `${BASE}/run/${test.id}`,
            method,
            headers,
            data: test.payload || "",
        }

        if (isAsync) {
            const response = axios.request(request);
            response.then(
                partial(onResponse, handler)
            );
            return response;
        }

        request.onDownloadProgress = partial(
            onDownloadProgress, handler
        );
        return axios.request(request);
    },
};