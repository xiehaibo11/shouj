import { listen } from "@tauri-apps/api/event";
import { getCurrentWebviewWindow } from "@tauri-apps/api/webviewWindow";
import { useEffect } from "react";
import { mutate } from "swr";

import { useListen } from "@/hooks/use-listen";
import { getAxios } from "@/services/api";

export const useLayoutEvents = (
  handleNotice: (payload: [string, string]) => void,
) => {
  const { addListener } = useListen();

  useEffect(() => {
    const unlisteners: Array<() => void> = [];
    let disposed = false;

    const register = (
      maybeUnlisten: void | (() => void) | Promise<void | (() => void)>,
    ) => {
      if (!maybeUnlisten) return;

      if (typeof maybeUnlisten === "function") {
        unlisteners.push(maybeUnlisten);
        return;
      }

      maybeUnlisten
        .then((unlisten) => {
          if (!unlisten) return;
          if (disposed) {
            unlisten();
          } else {
            unlisteners.push(unlisten);
          }
        })
        .catch((error) => console.error("[事件监听] 注册失败", error));
    };

    register(
      addListener("verge://refresh-clash-config", async () => {
        await getAxios(true);
        mutate("getProxies");
        mutate("getVersion");
        mutate("getClashConfig");
        mutate("getProxyProviders");
      }),
    );

    register(
      addListener("verge://refresh-verge-config", () => {
        mutate("getVergeConfig");
        mutate("getSystemProxy");
        mutate("getAutotemProxy");
        mutate("getRunningMode");
        mutate("isServiceAvailable");
      }),
    );

    register(
      addListener("verge://notice-message", ({ payload }) =>
        handleNotice(payload as [string, string]),
      ),
    );

    const appWindow = getCurrentWebviewWindow();
    register(
      (async () => {
        const [hideUnlisten, showUnlisten] = await Promise.all([
          listen("verge://hide-window", () => appWindow.hide()),
          listen("verge://show-window", () => appWindow.show()),
        ]);
        return () => {
          hideUnlisten();
          showUnlisten();
        };
      })(),
    );

    return () => {
      disposed = true;
      const errors: Error[] = [];

      unlisteners.forEach((unlisten) => {
        try {
          unlisten();
        } catch (error) {
          errors.push(
            error instanceof Error ? error : new Error(String(error)),
          );
        }
      });

      if (errors.length > 0) {
        console.error(
          `[事件监听] 清理过程中发生 ${errors.length} 个错误:`,
          errors,
        );
      }

      unlisteners.length = 0;
    };
  }, [addListener, handleNotice]);
};
