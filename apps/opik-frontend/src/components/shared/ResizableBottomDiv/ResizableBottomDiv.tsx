import React, { useRef, PointerEvent } from "react";
import useLocalStorageState from "use-local-storage-state";

interface ResizableBottomDiv {
  children: React.ReactNode;
  minHeight?: number;
  localStorageKey: string;
  defaultHeight?: number;
}

const ResizableBottomDiv = ({
  children,
  localStorageKey,
  minHeight = 200,
  defaultHeight = 200,
}: ResizableBottomDiv) => {
  const [height, setHeight] = useLocalStorageState<number>(localStorageKey, {
    defaultValue: defaultHeight,
  });

  const isResizing = useRef(false);
  const startY = useRef(0);
  const startHeight = useRef(0);

  const handlePointerDown = (e: PointerEvent<HTMLDivElement>) => {
    e.currentTarget.setPointerCapture(e.pointerId);
    isResizing.current = true;
    startY.current = e.clientY;
    startHeight.current = height;
  };

  const handlePointerMove = (e: PointerEvent<HTMLDivElement>) => {
    if (!isResizing.current) return;

    const deltaY = e.clientY - startY.current;
    const newHeight = startHeight.current + deltaY;
    setHeight(Math.max(minHeight, newHeight));
  };

  const handlePointerUp = (e: PointerEvent<HTMLDivElement>) => {
    isResizing.current = false;
    e.currentTarget.releasePointerCapture(e.pointerId);
  };

  return (
    <div
      className="box-border flex h-[var(--container-height)] min-h-[var(--container-height)] flex-col overflow-hidden"
      style={
        {
          "--container-height": `${height}px`,
        } as React.CSSProperties
      }
    >
      <div className="flex grow overflow-hidden">{children}</div>

      <div
        className="sticky bottom-0 mt-auto flex w-full cursor-row-resize border-b py-0.5"
        onPointerDown={handlePointerDown}
        onPointerMove={handlePointerMove}
        onPointerUp={handlePointerUp}
      />
    </div>
  );
};

export default ResizableBottomDiv;
