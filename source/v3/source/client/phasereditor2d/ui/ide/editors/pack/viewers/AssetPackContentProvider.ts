namespace phasereditor2d.ui.ide.editors.pack.viewers {

    export abstract class AssetPackContentProvider implements controls.viewers.ITreeContentProvider {

        abstract getRoots(input: any): any[];

        getChildren(parent: any): any[] {
            if (parent instanceof AssetPack) {
                return parent.getItems();
            }


            if (parent instanceof AssetPackItem) {
                
                if (parent.getType() === IMAGE_TYPE) {
                    return [];
                }

                if (AssetPackUtils.isImageFrameContainer(parent)) {
                    return AssetPackUtils.getImageFrames(parent);
                }
            }

            return [];
        }
    }
}