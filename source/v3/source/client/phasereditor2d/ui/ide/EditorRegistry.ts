namespace phasereditor2d.ui.ide {
    export class EditorRegistry {
        private _map: Map<string, EditorFactory>;

        public constructor() {
            this._map = new Map();
        }

        public registerFactory(factory: EditorFactory): void {
            this._map.set(factory.getId(), factory);
        }

        public getFactoryForInput(input: any): EditorFactory {
            for (const factory of this._map.values()) {
                if (factory.acceptInput(input)) {
                    return factory;
                }
            }

            return null;
        }
    }
}