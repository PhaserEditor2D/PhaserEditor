namespace phasereditor2d.ui.ide.editors.pack.viewers {

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
                        const parser = new parsers.MultiAtlasParser(parent);
                        const frames = parser.parse();
                        return frames;
                    }
                    case "atlas": {
                        const parser = new parsers.AtlasParser(parent);
                        const frames = parser.parse();
                        return frames;
                    }
                    case "unityAtlas": {
                        const parser = new parsers.UnityAtlasParser(parent);
                        const frames = parser.parse();
                        return frames;
                    }
                    case "atlasXML": {
                        const parser = new parsers.AtlasXMLParser(parent);
                        const frames = parser.parse();
                        return frames;
                    }
                    case "spritesheet": {
                        const parser = new parsers.SpriteSheetParser(parent);
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