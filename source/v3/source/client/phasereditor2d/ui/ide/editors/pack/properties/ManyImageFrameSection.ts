namespace phasereditor2d.ui.ide.editors.pack.properties {

    export class ManyImageFrameSection extends controls.properties.PropertySection<any> {

        protected createForm(parent: HTMLDivElement) {
            
        }

        

        canEdit(obj: any): boolean {
            return obj instanceof ImageFrame || obj instanceof AssetPackItem && AssetPackUtils.isImageFrameContainer(obj);
        }

        canEditNumber(n: number): boolean {
            return n > 1;
        }

    }

}