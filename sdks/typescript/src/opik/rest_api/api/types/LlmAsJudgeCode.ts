/**
 * This file was auto-generated by Fern from our API Definition.
 */

import * as OpikApi from "../index";

export interface LlmAsJudgeCode {
    model: OpikApi.LlmAsJudgeModelParameters;
    messages: OpikApi.LlmAsJudgeMessage[];
    variables: Record<string, string>;
    schema: OpikApi.LlmAsJudgeOutputSchema[];
}
