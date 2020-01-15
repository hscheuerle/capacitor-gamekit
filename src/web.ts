import { WebPlugin } from '@capacitor/core';
import { MyPlayServicesPlugin } from './definitions';

export class MyPlayServicesWeb extends WebPlugin implements MyPlayServicesPlugin {
  constructor() {
    super({
      name: 'MyPlayServices',
      platforms: ['web']
    });
  }

  async echo(options: { value: string }): Promise<{value: string}> {
    console.log('ECHO', options);
    return options;
  }
}

const MyPlayServices = new MyPlayServicesWeb();

export { MyPlayServices };

import { registerWebPlugin } from '@capacitor/core';
registerWebPlugin(MyPlayServices);
