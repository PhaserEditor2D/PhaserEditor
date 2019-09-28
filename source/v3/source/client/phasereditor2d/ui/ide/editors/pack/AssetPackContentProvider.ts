namespace phasereditor2d.ui.ide.editors.pack {

    export abstract class AssetPackContentProvider implements controls.viewers.ITreeContentProvider {

        abstract getRoots(input: any): any[];

        getChildren(parent: any): any[] {
            if (parent instanceof AssetPack) {
                return parent.getItems();
            }

            if (parent instanceof AssetPackItem) {
                const type = parent.getType();

                switch (type) {
                    case "multiatlas": {
                        const parser = new MultiAtlasParser(parent);
                        const frames = parser.parse();
                        return frames;
                    }
                    case "atlas": {
                        const parser = new AtlasParser(parent);
                        const frames = parser.parse();
                        return frames;
                    }
                    default:
                        break;
                }
            }

            return [];
        }
    }
}