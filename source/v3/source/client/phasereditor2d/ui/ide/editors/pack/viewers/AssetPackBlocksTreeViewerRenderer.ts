/// <reference path="../../../../../../phasereditor2d.ui.controls/viewers/GridTreeViewerRenderer.ts" />

namespace phasereditor2d.ui.ide.editors.pack.viewers {

    export class AssetPackBlocksTreeViewerRenderer extends controls.viewers.GridTreeViewerRenderer {

        constructor(viewer: controls.viewers.TreeViewer) {
            super(viewer, false);
            viewer.setCellSize(64);
        }

        renderCellBack(args: controls.viewers.RenderCellArgs, selected: boolean) {
            super.renderCellBack(args, selected);

            const isParent = this.isParent(args.obj);
            const isChild = this.isChild(args.obj);

            if (isParent || isChild) {
                const margin = isChild? controls.viewers.TREE_RENDERER_GRID_PADDING : 0;
                const ctx = args.canvasContext;
                ctx.save();
                ctx.fillStyle = "rgba(0, 0, 0, 0.2)";
                ctx.fillRect(args.x - margin, args.y, args.w + margin, args.h);
                ctx.restore();
            }
        }

        protected isParent(obj: any) {
            if (obj instanceof pack.AssetPackItem) {
                switch (obj.getType()) {
                    case "atlas":
                    case "multiatlas":
                    case "atlasXML":
                    case "unityAtlas":
                        return true;
                    default:
                        return false;
                }
            }
            return false;
        }

        protected isChild(obj: any) {
            return obj instanceof ImageFrame;
        }
    }
}