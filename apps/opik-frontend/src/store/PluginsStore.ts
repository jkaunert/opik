import React from "react";
import { create } from "zustand";

import WorkspacePreloader from "@/components/shared/WorkspacePreloader/WorkspacePreloader";

type PluginStore = {
  Logo: React.ComponentType<{ expanded: boolean }> | null;
  UserMenu: React.ComponentType | null;
  GetStartedPage: React.ComponentType | null;
  QuickstartPage: React.ComponentType | null;
  WorkspacePreloader: React.ComponentType<{ children: React.ReactNode }> | null;
  init: unknown;
  setupPlugins: (folderName: string) => Promise<void>;
};

const VALID_PLUGIN_FOLDER_NAMES = ["comet"];
const PLUGIN_NAMES = [
  "Logo",
  "UserMenu",
  "GetStartedPage",
  "QuickstartPage",
  "WorkspacePreloader",
  "init",
];

const usePluginsStore = create<PluginStore>((set) => ({
  Logo: null,
  UserMenu: null,
  GetStartedPage: null,
  QuickstartPage: null,
  WorkspacePreloader: null,
  init: null,
  setupPlugins: async (folderName: string) => {
    if (!VALID_PLUGIN_FOLDER_NAMES.includes(folderName)) {
      return set({ WorkspacePreloader });
    }

    for (const pluginName of PLUGIN_NAMES) {
      try {
        // dynamic import does not support alias
        const plugin = await import(
          `../plugins/${folderName}/${pluginName}.tsx`
        );

        if (plugin.default) {
          set({ [pluginName]: plugin.default });
        }
      } catch (error) {
        continue;
      }
    }
  },
}));

export default usePluginsStore;
