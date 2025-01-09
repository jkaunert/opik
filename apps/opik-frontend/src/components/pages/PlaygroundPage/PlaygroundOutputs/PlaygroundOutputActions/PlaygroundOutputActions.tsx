import React, {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import useDatasetsList from "@/api/datasets/useDatasetsList";
import { Dataset, DatasetItem } from "@/types/datasets";
import { Button } from "@/components/ui/button";
import { Pause, Play, X } from "lucide-react";
import TooltipWrapper from "@/components/shared/TooltipWrapper/TooltipWrapper";

import {
  usePromptCount,
  usePromptMap,
  useResetOutputMap,
} from "@/store/PlaygroundStore";

import LoadableSelectBox from "@/components/shared/LoadableSelectBox/LoadableSelectBox";
import { useLocation } from "@tanstack/react-router";
import useActionButtonActions from "@/components/pages/PlaygroundPage/PlaygroundOutputs/PlaygroundOutputActions/useActionButtonActions";

const EMPTY_DATASETS: Dataset[] = [];

interface PlaygroundOutputActionsProps {
  datasetId: string | null;
  onChangeDatasetId: (id: string | null) => void;
  workspaceName: string;
  datasetItems: DatasetItem[];
  loadingDatasetItems: boolean;
}

const DEFAULT_LOADED_DATASETS = 25;
const RUN_HOT_KEYS = ["⌘", "⏎"];

const PlaygroundOutputActions = ({
  datasetId,
  onChangeDatasetId,
  workspaceName,
  datasetItems,
  loadingDatasetItems,
}: PlaygroundOutputActionsProps) => {
  const actionButtonRef = useRef<HTMLButtonElement>(null);
  const location = useLocation();

  const [isLoadedMore, setIsLoadedMore] = useState(false);

  const promptMap = usePromptMap();
  const promptCount = usePromptCount();
  const resetOutputMap = useResetOutputMap();

  const {
    data: datasetsData,
    isLoading: isLoadingDatasets,
    isPending: isPendingDatasets,
  } = useDatasetsList({
    workspaceName,
    page: 1,
    size: !isLoadedMore ? DEFAULT_LOADED_DATASETS : 1000,
  });

  const datasets = datasetsData?.content || EMPTY_DATASETS;
  const datasetTotal = datasetsData?.total;

  const datasetOptions = useMemo(() => {
    return datasets.map((ds) => ({
      label: ds.name,
      value: ds.id,
    }));
  }, [datasets]);

  const { stopAll, runAll, isRunning } = useActionButtonActions({
    workspaceName,
    datasetItems,
  });

  const loadMoreHandler = useCallback(() => setIsLoadedMore(true), []);

  const handleChangeDatasetId = useCallback(
    (id: string | null) => {
      if (datasetId !== id) {
        onChangeDatasetId(id);
        resetOutputMap();
        stopAll();
      }
    },
    [onChangeDatasetId, resetOutputMap, stopAll, datasetId],
  );

  const renderActionButton = () => {
    if (isRunning) {
      const stopRunningPromptMessage =
        promptCount === 1 ? "Stop a running prompt" : "Stop running prompts";

      return (
        <TooltipWrapper
          content={stopRunningPromptMessage}
          hotkeys={RUN_HOT_KEYS}
        >
          <Button
            size="sm"
            className="mt-2.5"
            variant="outline"
            onClick={stopAll}
            ref={actionButtonRef}
          >
            <Pause className="mr-1 size-4" />
            Stop all
          </Button>
        </TooltipWrapper>
      );
    }

    const areAllPromptsValid = Object.values(promptMap).every((p) => !!p.model);
    const isDatasetEmpty = datasetId && datasetItems.length === 0;
    const isDatasetRemoved = !datasets.find((d) => d.id === datasetId);

    const isDisabled =
      !areAllPromptsValid ||
      loadingDatasetItems ||
      isLoadingDatasets ||
      isDatasetRemoved ||
      !!isDatasetEmpty;

    const style: React.CSSProperties = isDisabled
      ? { pointerEvents: "auto" }
      : {};

    const selectLLMModelMessage =
      promptCount === 1
        ? "Please select a LLM model for your prompt"
        : "Please select a LLM model for your prompts";

    const datasetRemovedMessage = isDatasetRemoved
      ? "Your dataset has been removed. Select another one"
      : "";

    const emptyDatasetMessage = isDatasetEmpty
      ? "Selected dataset is empty"
      : "";

    const runMessage =
      promptCount === 1 ? "Run your prompt" : "Run your prompts";

    const tooltipMessage = isDisabled
      ? datasetRemovedMessage || emptyDatasetMessage || selectLLMModelMessage
      : runMessage;

    return (
      <TooltipWrapper
        content={tooltipMessage}
        hotkeys={isDisabled ? undefined : RUN_HOT_KEYS}
      >
        <Button
          size="sm"
          className="mt-2.5"
          onClick={runAll}
          disabled={isDisabled}
          style={style}
          ref={actionButtonRef}
        >
          <Play className="mr-1 size-4" />
          Run all
        </Button>
      </TooltipWrapper>
    );
  };

  useEffect(() => {
    // stop streaming whenever the location changes
    return () => stopAll();
  }, [location, stopAll]);

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if ((event.metaKey || event.ctrlKey) && event.key === "Enter") {
        event.preventDefault();
        event.stopPropagation();

        if (isRunning) {
          stopAll();
        } else {
          runAll();
        }

        actionButtonRef.current?.focus();
      }
    };

    window.addEventListener("keydown", handleKeyDown, true);

    return () => {
      window.removeEventListener("keydown", handleKeyDown, true);
    };
  }, [runAll, isRunning, stopAll]);

  return (
    <div className="sticky right-0 ml-auto flex h-0 gap-2">
      <div className="mt-2.5 flex">
        <LoadableSelectBox
          options={datasetOptions}
          value={datasetId || ""}
          placeholder="Select a dataset"
          onChange={handleChangeDatasetId}
          buttonSize="sm"
          onLoadMore={
            (datasetTotal || 0) > DEFAULT_LOADED_DATASETS && !isLoadedMore
              ? loadMoreHandler
              : undefined
          }
          isLoading={isLoadingDatasets}
          optionsCount={DEFAULT_LOADED_DATASETS}
          buttonClassName="w-48 rounded-r-none"
        />

        <Button
          variant="outline"
          size="icon-sm"
          className="border-l-0 rounded-l-none"
          onClick={() => handleChangeDatasetId(null)}
        >
          <X className="size-4 text-light-slate" />
        </Button>
      </div>

      {renderActionButton()}
    </div>
  );
};

export default PlaygroundOutputActions;
