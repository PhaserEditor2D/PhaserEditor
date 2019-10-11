namespace phasereditor2d.ui.ide.editors.pack.viewers {

    export class AssetPackBlocksTreeViewerRenderer extends controls.viewers.GridTreeViewerRenderer {

        constructor(viewer: controls.viewers.TreeViewer) {
            super(viewer, false);
            
            viewer.setCellSize(64);
        }

        renderCellBack(args: controls.viewers.RenderCellArgs, selected: boolean, isLastChild: boolean) {

            super.renderCellBack(args, selected, isLastChild);

            const isParent = this.isParent(args.obj);
            const isChild = this.isChild(args.obj);
            const expanded = args.viewer.isExpanded(args.obj);

            if (isParent) {

                const ctx = args.canvasContext;

                ctx.save();

                ctx.fillStyle = "rgba(0, 0, 0, 0.2)";

                if (expanded) {
                    controls.Controls.drawRoundedRect(ctx, args.x, args.y, args.w, args.h, 5, 0, 0, 5);
                } else {
                    controls.Controls.drawRoundedRect(ctx, args.x, args.y, args.w, args.h, 5, 5, 5, 5);
                }

                ctx.restore();

            } else if (isChild) {

                const margin = controls.viewers.TREE_RENDERER_GRID_PADDING;

                const ctx = args.canvasContext;

                ctx.save();

                ctx.fillStyle = "rgba(0, 0, 0, 0.2)";

                if (isLastChild) {

                    controls.Controls.drawRoundedRect(ctx, args.x - margin, args.y, args.w + margin, args.h, 0, 5, 5, 0);

                } else {

                    controls.Controls.drawRoundedRect(ctx, args.x - margin, args.y, args.w + margin, args.h, 0, 0, 0, 0);

                }

                ctx.restore();
            }
        }

        protected isParent(obj: any) {

            if (obj instanceof pack.AssetPackItem) {

                switch (obj.getType()) {
                    case pack.ATLAS_TYPE:
                    case pack.MULTI_ATLAS_TYPE:
                    case pack.ATLAS_XML_TYPE:
                    case pack.UNITY_ATLAS_TYPE:
                    case pack.SPRITESHEET_TYPE:
                        return true;
                    default:
                        return false;
                }

            }

            return false;
        }

        protected isChild(obj: any) {
            return obj instanceof controls.ImageFrame;
        }
    }
}