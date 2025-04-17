import { startsWith } from 'lodash';
import { JMAAS_HEADERS } from '../api/jmaas.ts';
import type { TestStreamEvent } from '../api/jmaas.ts';
import type {
    CatalogTest,
} from '../types/catalogs.ts'
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