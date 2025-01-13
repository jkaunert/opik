/**
 * This file was auto-generated by Fern from our API Definition.
 */

import * as serializers from "../index";
import * as OpikApi from "../../api/index";
import * as core from "../../core";

export const LlmAsJudgeModelParametersPublic: core.serialization.ObjectSchema<
    serializers.LlmAsJudgeModelParametersPublic.Raw,
    OpikApi.LlmAsJudgeModelParametersPublic
> = core.serialization.object({
    name: core.serialization.string(),
    temperature: core.serialization.number(),
});

export declare namespace LlmAsJudgeModelParametersPublic {
    interface Raw {
        name: string;
        temperature: number;
    }
}
