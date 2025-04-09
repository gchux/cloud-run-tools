import { z } from 'zod'
import axios from 'axios';
import { toString, join, isEqual } from 'lodash'
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

type Headers = z.infer<typeof HeadersSchema>;

export default {
    getCatalog: (catalog: string = "default") => {
        return axios.get(`${BASE}/catalog/${catalog}`);
    },

    runTest: (test: Test) => {
        const mode = test.mode;
        const method = test.method;
        const headers: Headers = {};

        headers[`${headersPrefix}-mode`] = test.mode;
        headers[`${headersPrefix}-script`] = test.script;
        headers[`${headersPrefix}-method`] = test.method;
        headers[`${headersPrefix}-host`] = test.host;
        headers[`${headersPrefix}-port`] = toString(test.port);
        headers[`${headersPrefix}-path`] = test.path;
        headers[`${headersPrefix}-async`] = toString(test.async);
        headers[`${headersPrefix}-duration`] = toString(test.duration);
        headers[`${headersPrefix}-min-latency`] = toString(test.minLatency);
        headers[`${headersPrefix}-max-latency`] = toString(test.maxLatency);
        headers[`${headersPrefix}-query`] = getKeyValueParam(test, KeyValueParamsSchema.Values.query);
        headers[`${headersPrefix}-headers`] = getKeyValueParam(test, KeyValueParamsSchema.Values.headers);

        if ( isEqual(mode, "qps") ) {
            headers[`${headersPrefix}-qps`] = getMultiValueParam(test, MultiValueParamsSchema.Values.qps);
        } else {
            headers[`${headersPrefix}-concurrency`] = getMultiValueParam(test, MultiValueParamsSchema.Values.concurrency);
        }

        return axios.request({
            url: `${BASE}/run`,
            method,
            headers,
        });
    },
};