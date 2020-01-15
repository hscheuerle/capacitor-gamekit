declare module "@capacitor/core" {
  interface PluginRegistry {
    MyPlayServices: MyPlayServicesPlugin;
  }
}

export interface MyPlayServicesPlugin {
  echo(options: { value: string }): Promise<{value: string}>;
}
