package com.comet.opik.domain.llmproviders;

import com.comet.opik.infrastructure.LlmProviderClientConfig;
import dev.ai4j.openai4j.chat.AssistantMessage;
import dev.ai4j.openai4j.chat.ChatCompletionChoice;
import dev.ai4j.openai4j.chat.ChatCompletionRequest;
import dev.ai4j.openai4j.chat.ChatCompletionResponse;
import dev.ai4j.openai4j.chat.Message;
import dev.ai4j.openai4j.chat.Role;
import dev.ai4j.openai4j.chat.UserMessage;
import dev.ai4j.openai4j.shared.Usage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.anthropic.internal.api.AnthropicContent;
import dev.langchain4j.model.anthropic.internal.api.AnthropicCreateMessageRequest;
import dev.langchain4j.model.anthropic.internal.api.AnthropicCreateMessageResponse;
import dev.langchain4j.model.anthropic.internal.api.AnthropicMessage;
import dev.langchain4j.model.anthropic.internal.api.AnthropicMessageContent;
import dev.langchain4j.model.anthropic.internal.api.AnthropicRole;
import dev.langchain4j.model.anthropic.internal.api.AnthropicTextContent;
import dev.langchain4j.model.anthropic.internal.api.AnthropicToolChoice;
import dev.langchain4j.model.anthropic.internal.client.AnthropicClient;
import dev.langchain4j.model.anthropic.internal.client.AnthropicHttpException;
import dev.langchain4j.model.output.Response;
import jakarta.ws.rs.BadRequestException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
public class Anthropic implements LlmProviderService {
    private final LlmProviderClientConfig llmProviderClientConfig;
    private final AnthropicClient anthropicClient;

    public Anthropic(LlmProviderClientConfig llmProviderClientConfig, String apiKey) {
        this.llmProviderClientConfig = llmProviderClientConfig;
        this.anthropicClient = newClient(apiKey);
    }

    @Override
    public ChatCompletionResponse generate(@NonNull ChatCompletionRequest request, @NonNull String workspaceId) {
        var response = anthropicClient.createMessage(mapToAnthropicCreateMessageRequest(request));

        return ChatCompletionResponse.builder()
                .id(response.id)
                .model(response.model)
                .choices(response.content.stream().map(content -> mapContentToChoice(response, content))
                        .toList())
                .usage(Usage.builder()
                        .promptTokens(response.usage.inputTokens)
                        .completionTokens(response.usage.outputTokens)
                        .totalTokens(response.usage.inputTokens + response.usage.outputTokens)
                        .build())
                .build();
    }

    @Override
    public void generateStream(
            @NonNull ChatCompletionRequest request,
            @NonNull String workspaceId,
            @NonNull Consumer<ChatCompletionResponse> handleMessage,
            @NonNull Runnable handleClose, @NonNull Consumer<Throwable> handleError) {
        validateRequest(request);
        anthropicClient.createMessage(mapToAnthropicCreateMessageRequest(request),
                new ChunkedResponseHandler(handleMessage, handleClose, handleError));
    }

    @Override
    public void validateRequest(ChatCompletionRequest request) {
        // see https://github.com/anthropics/courses/blob/master/anthropic_api_fundamentals/04_parameters.ipynb
        if (CollectionUtils.isEmpty(request.messages())) {
            throw new BadRequestException("messages cannot be empty");
        }
        if (request.maxCompletionTokens() == null) {
            throw new BadRequestException("maxCompletionTokens cannot be null");
        }
        if (StringUtils.isEmpty(request.model())) {
            throw new BadRequestException("model cannot be empty");
        }
    }

    @Override
    public Class<? extends Throwable> getHttpExceptionClass() {
        return AnthropicHttpException.class;
    }

    @Override
    public int getHttpErrorStatusCode(Throwable throwable) {
        if (throwable instanceof AnthropicHttpException anthropicHttpException) {
            return anthropicHttpException.statusCode();
        }

        return 500;
    }

    private AnthropicCreateMessageRequest mapToAnthropicCreateMessageRequest(ChatCompletionRequest request) {
        var builder = AnthropicCreateMessageRequest.builder();
        Optional.ofNullable(request.toolChoice())
                .ifPresent(toolChoice -> builder.toolChoice(AnthropicToolChoice.from(request.toolChoice().toString())));
        return builder
                .model(request.model())
                .messages(request.messages().stream().map(this::mapMessage).toList())
                .temperature(request.temperature())
                .topP(request.topP())
                .stopSequences(request.stop())
                .maxTokens(request.maxCompletionTokens())
                .build();
    }

    private AnthropicMessage mapMessage(Message message) {
        if (message.role() == Role.ASSISTANT) {
            return AnthropicMessage.builder()
                    .role(AnthropicRole.ASSISTANT)
                    .content(List.of(new AnthropicTextContent(((AssistantMessage) message).content())))
                    .build();
        } else if (message.role() == Role.USER) {
            return AnthropicMessage.builder()
                    .role(AnthropicRole.USER)
                    .content(List.of(mapMessageContent(((UserMessage) message).content())))
                    .build();
        }

        // Anthropic only supports assistant and user roles
        throw new BadRequestException("not supported message role: " + message.role());
    }

    private AnthropicMessageContent mapMessageContent(Object rawContent) {
        if (rawContent instanceof String content) {
            return new AnthropicTextContent(content);
        }

        throw new BadRequestException("only text content is supported");
    }

    private ChatCompletionChoice mapContentToChoice(AnthropicCreateMessageResponse response, AnthropicContent content) {
        return ChatCompletionChoice.builder()
                .message(AssistantMessage.builder()
                        .name(content.name)
                        .content(content.text)
                        .build())
                .finishReason(response.stopReason)
                .build();
    }

    private AnthropicClient newClient(String apiKey) {
        var anthropicClientBuilder = AnthropicClient.builder();
        Optional.ofNullable(llmProviderClientConfig.getAnthropicClient())
                .map(LlmProviderClientConfig.AnthropicClientConfig::baseUrl)
                .ifPresent(baseUrl -> {
                    if (StringUtils.isNotBlank(baseUrl)) {
                        anthropicClientBuilder.baseUrl(baseUrl);
                    }
                });
        Optional.ofNullable(llmProviderClientConfig.getAnthropicClient())
                .map(LlmProviderClientConfig.AnthropicClientConfig::version)
                .ifPresent(version -> {
                    if (StringUtils.isNotBlank(version)) {
                        anthropicClientBuilder.version(version);
                    }
                });
        Optional.ofNullable(llmProviderClientConfig.getAnthropicClient())
                .map(LlmProviderClientConfig.AnthropicClientConfig::logRequests)
                .ifPresent(anthropicClientBuilder::logRequests);
        Optional.ofNullable(llmProviderClientConfig.getAnthropicClient())
                .map(LlmProviderClientConfig.AnthropicClientConfig::logResponses)
                .ifPresent(anthropicClientBuilder::logResponses);
        Optional.ofNullable(llmProviderClientConfig.getCallTimeout())
                .ifPresent(callTimeout -> anthropicClientBuilder.timeout(callTimeout.toJavaDuration()));
        return anthropicClientBuilder
                .apiKey(apiKey)
                .build();
    }

    private static class ChunkedResponseHandler implements StreamingResponseHandler<AiMessage> {
        private final Consumer<ChatCompletionResponse> handleMessage;
        private final Runnable handleClose;
        private final Consumer<Throwable> handleError;

        public ChunkedResponseHandler(
                Consumer<ChatCompletionResponse> handleMessage, Runnable handleClose, Consumer<Throwable> handleError) {
            this.handleMessage = handleMessage;
            this.handleClose = handleClose;
            this.handleError = handleError;
        }

        @Override
        public void onNext(String s) {
            log.info("received chunked response: {}", s);
        }

        @Override
        public void onComplete(Response<AiMessage> response) {
            StreamingResponseHandler.super.onComplete(response);
        }

        @Override
        public void onError(Throwable throwable) {
            handleError.accept(throwable);
        }
    }
}
