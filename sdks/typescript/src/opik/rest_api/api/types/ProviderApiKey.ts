/**
 * This file was auto-generated by Fern from our API Definition.
 */

import * as OpikApi from "../index";

export interface ProviderApiKey {
    id?: string;
    provider: OpikApi.ProviderApiKeyProvider;
    apiKey: string;
    name?: string;
    createdAt?: Date;
    createdBy?: string;
    lastUpdatedAt?: Date;
    lastUpdatedBy?: string;
}
