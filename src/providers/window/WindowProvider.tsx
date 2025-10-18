import { getCurrentWindow } from "@tauri-apps/api/window";
import React, { useCallback, useEffect, useMemo, useState } from "react";

import debounce from "@/utils/debounce";

import { WindowContext } from "./WindowContext";

export const WindowProvider: React.FC<{ children: React.ReactNode }> = ({
  children,
}) => {
  const currentWindow = useMemo(() => getCurrentWindow(), []);
  const [decorated, setDecorated] = useState<boolean | null>(null);
  const [maximized, setMaximized] = useState<boolean | null>(null);

  const close = useCallback(() => currentWindow.close(), [currentWindow]);
  const minimize = useCallback(() => currentWindow.minimize(), [currentWindow]);

  useEffect(() => {
    const checkMaximized = debounce(async () => {
      const value = await currentWindow.isMaximized();
      setMaximized(value);
    }, 300);

    const unlistenPromise = currentWindow.onResized(checkMaximized);

    return () => {
      unlistenPromise.then((unlisten) => unlisten());
    };
  }, [currentWindow]);

  const toggleMaximize = useCallback(async () => {
    if (await currentWindow.isMaximized()) {
      await currentWindow.unmaximize();
      setMaximized(false);
    } else {
      await currentWindow.maximize();
      setMaximized(true);
    }
  }, [currentWindow]);

  const toggleFullscreen = useCallback(async () => {
    await currentWindow.setFullscreen(!(await currentWindow.isFullscreen()));
  }, [currentWindow]);

  const refreshDecorated = useCallback(async () => {
    const val = await currentWindow.isDecorated();
    setDecorated(val);
    return val;
  }, [currentWindow]);

  const toggleDecorations = useCallback(async () => {
    const currentVal = await currentWindow.isDecorated();
    await currentWindow.setDecorations(!currentVal);
    setDecorated(!currentVal);
  }, [currentWindow]);

  useEffect(() => {
    refreshDecorated();
    currentWindow.setMinimizable?.(true);
  }, [currentWindow, refreshDecorated]);

  const contextValue = useMemo(
    () => ({
      decorated,
      maximized,
      toggleDecorations,
      refreshDecorated,
      minimize,
      close,
      toggleMaximize,
      toggleFullscreen,
      currentWindow,
    }),
    [
      decorated,
      maximized,
      toggleDecorations,
      refreshDecorated,
      minimize,
      close,
      toggleMaximize,
      toggleFullscreen,
      currentWindow,
    ],
  );

  return <WindowContext value={contextValue}>{children}</WindowContext>;
};
