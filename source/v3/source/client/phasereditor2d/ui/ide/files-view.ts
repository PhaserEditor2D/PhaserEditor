/// <reference path="./parts.ts"/>

namespace phasereditor2d.ui.files {

    import io = phasereditor2d.core.io;
    import viewers = phasereditor2d.ui.controls.viewers;

    class FileTreeContentProvider implements viewers.ITreeContentProvider {

        getRoots(input: any): any[] {
            return this.getChildren(input);
        }

        getChildren(parent: any): any[] {
            return (<io.FilePath>parent).getFiles();
        }

    }

    class FileCellRenderer extends viewers.LabelCellRenderer {

        getLabel(obj: any): string {
            return (<io.FilePath>obj).getName();
        }
    }

    class FileCellRendererProvider implements viewers.ICellRendererProvider {
        getCellRenderer(element: any): viewers.ICellRenderer {
            return new FileCellRenderer();
        }
    }


    export class FilesView extends parts.ViewPart {
        constructor() {
            super("filesView");
            this.setTitle("Files");

            let root = new core.io.FilePath(null, {
                name: "<root>",
                isFile: false,
                children: [
                    {
                        name: "index.html",
                        isFile: true
                    },
                    {
                        name: "assets",
                        isFile: false,
                        children: [
                            {
                                name: "bg.png",
                                isFile: true
                            }
                        ]
                    }
                ]
            });

            console.log(root.toStringTree());

            let tree = new viewers.TreeViewer();
            tree.setContentProvider(new FileTreeContentProvider());
            tree.setCellRendererProvider(new FileCellRendererProvider());
            tree.setInput(root);

            this.getClientArea().setLayout(new ui.controls.FillLayout());
            this.getClientArea().add(tree);

            tree.repaint();
        }
    }
}