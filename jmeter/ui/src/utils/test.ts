import { startsWith, replace, toString, split, toNumber } from 'lodash';
import { JMAAS_HEADERS } from '../api/jmaas.ts';
import type { TestStreamEvent } from '../api/jmaas.ts';
import {
    MultiValueParamsSchema,
    type CatalogTest,
    type CatalogTestParam,
    type MultiValueParamType,
} from '../types/catalogs.ts'
import type { ModelValue } from '../components/MultiValueInput.vue'
import type { TestData } from '@/types/test.ts';

const headerValue = (
    data: TestStreamEvent,
    name: string,
): string => {
    return data.headers[name];
};

export const isCloudRun = (
    test: CatalogTest
): boolean => {
    return startsWith(test.id, "cloud_run_");
};

export const getTestID = (
    data: TestStreamEvent,
): string => {
    return headerValue(data, JMAAS_HEADERS.TEST_ID);
};

export const getTraceID = (
    data: TestStreamEvent,
): string => {
    return headerValue(data, JMAAS_HEADERS.TRACE_ID);
};

export const getInstanceID = (
    data: TestStreamEvent,
): string => {
    return headerValue(data, JMAAS_HEADERS.INSTANCE_ID);
};

export const onTestData = (
    data: TestData,
    event: TestStreamEvent,
) => {
    data.traceID = getTraceID(event);
    data.instanceID = getInstanceID(event);
    data.output = event.response;
};

export const cleanTestOutput = (
    data: string,
) => {
    // reduce noise from non-relevant output entries
    return replace(
        toString(data),
        /^.*?\sINFO\s.*?\.(?:JMeterThread|VariableThroughputTimer|ClassFinder):\s.*[\r\n]+/gm,
        "",
    );
};

export const toTestParam = (
    param: CatalogTestParam,
    data: ModelValue,
): MultiValueParamType => {
    const parts = split(data.value, ',');

    switch (param.id) {
        case MultiValueParamsSchema.Enum.qps:
            if (parts.length == 3) {
                return {
                    startQPS: toNumber(parts[0]),
                    endQPS: toNumber(parts[1]),
                    duration: toNumber(parts[2]),
                };
            }
            break;

        case MultiValueParamsSchema.Enum.concurrency:
            if (parts.length == 5) {
                return {
                    threadCount: toNumber(parts[0]),
                    initialDelay: toNumber(parts[1]),
                    rampupTime: toNumber(parts[2]),
                    duration: toNumber(parts[3]),
                    shutdownTime: toNumber(parts[4]),
                };
            }
            break;

        default:
            throw new Error(`invalid multi value parameter: '${param.id}'`);
    }
    throw new Error(`invalid shape for '${param.id}': '[${data.value}]'`);
};