/// <reference path="../../../phasereditor2d.ui.controls/Controls.ts"/>
/// <reference path="../../../phasereditor2d.ui.controls/viewers/Viewer.ts"/>
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

        getImage(obj: any): controls.IIcon {
            const file = <io.FilePath>obj;
            if (file.isFile()) {
                const type = file.getContentType();
                const icon = Workbench.getWorkbench().getContentTypeIcon(type);
                if (icon) {
                    return icon;
                }
            } else {
                return controls.Controls.getIcon(controls.Controls.ICON_FOLDER);
            }

            return controls.Controls.getIcon(controls.Controls.ICON_FILE);
        }
    }

    class FileImageRenderer extends viewers.ImageCellRenderer {
        
        getLabel(obj: io.FilePath): string {
            return obj.getName();
        } 
        
        getImage(obj: io.FilePath): controls.IImage {
            return controls.Controls.getImage("files/" + obj.getFullName());
        }


    }

    class FileCellRendererProvider implements viewers.ICellRendererProvider {
        getCellRenderer(element: io.FilePath): viewers.ICellRenderer {
            if (element.getContentType() === "img") {
                return new FileImageRenderer();
            }
            return new FileCellRenderer();
        }
    }


    export class FilesView extends parts.ViewPart {
        constructor() {
            super("filesView");
            this.setTitle("Files");

            let root = new core.io.FilePath(null, TEST_DATA);

            //console.log(root.toStringTree());

            let tree = new viewers.TreeViewer();
            tree.setContentProvider(new FileTreeContentProvider());
            tree.setCellRendererProvider(new FileCellRendererProvider());
            tree.setInput(root);

            this.getClientArea().setLayout(new ui.controls.FillLayout());
            this.getClientArea().add(tree);

            tree.repaint();
        }
    }

    const TEST_DATA = {
        "name": "",
        "isFile": false,
        "children": [
            {
                "name": ".gitignore",
                "isFile": true,
                "contentType": "any"
            },
            {
                "name": "COPYRIGHTS",
                "isFile": true,
                "contentType": "any"
            },
            {
                "name": "assets",
                "isFile": false,
                "children": [
                    {
                        "name": "animations.json",
                        "isFile": true,
                        "contentType": "json"
                    },
                    {
                        "name": "atlas",
                        "isFile": false,
                        "children": [
                            {
                                "name": ".DS_Store",
                                "isFile": true,
                                "contentType": "any"
                            },
                            {
                                "name": "atlas-props.json",
                                "isFile": true,
                                "contentType": "json"
                            },
                            {
                                "name": "atlas-props.png",
                                "isFile": true,
                                "contentType": "img"
                            },
                            {
                                "name": "atlas.json",
                                "isFile": true,
                                "contentType": "json"
                            },
                            {
                                "name": "atlas.png",
                                "isFile": true,
                                "contentType": "img"
                            },
                            {
                                "name": "hello.atlas",
                                "isFile": true,
                                "contentType": "any"
                            },
                            {
                                "name": "hello.json",
                                "isFile": true,
                                "contentType": "json"
                            },
                            {
                                "name": "hello.png",
                                "isFile": true,
                                "contentType": "img"
                            }
                        ]
                    },
                    {
                        "name": "environment",
                        "isFile": false,
                        "children": [
                            {
                                "name": ".DS_Store",
                                "isFile": true,
                                "contentType": "any"
                            },
                            {
                                "name": "bg-clouds.png",
                                "isFile": true,
                                "contentType": "img"
                            },
                            {
                                "name": "bg-mountains.png",
                                "isFile": true,
                                "contentType": "img"
                            },
                            {
                                "name": "bg-trees.png",
                                "isFile": true,
                                "contentType": "img"
                            },
                            {
                                "name": "tileset.png",
                                "isFile": true,
                                "contentType": "img"
                            }
                        ]
                    },
                    {
                        "name": "fonts",
                        "isFile": false,
                        "children": [
                            {
                                "name": "arcade.png",
                                "isFile": true,
                                "contentType": "img"
                            },
                            {
                                "name": "arcade.xml",
                                "isFile": true,
                                "contentType": "any"
                            },
                            {
                                "name": "atari-classic.png",
                                "isFile": true,
                                "contentType": "img"
                            },
                            {
                                "name": "atari-classic.xml",
                                "isFile": true,
                                "contentType": "any"
                            }
                        ]
                    },
                    {
                        "name": "html",
                        "isFile": false,
                        "children": [
                            {
                                "name": "hello.html",
                                "isFile": true,
                                "contentType": "any"
                            }
                        ]
                    },
                    {
                        "name": "levels-pack-1.json",
                        "isFile": true,
                        "contentType": "json"
                    },
                    {
                        "name": "maps",
                        "isFile": false,
                        "children": [
                            {
                                "name": ".DS_Store",
                                "isFile": true,
                                "contentType": "any"
                            },
                            {
                                "name": "map.json",
                                "isFile": true,
                                "contentType": "json"
                            }
                        ]
                    },
                    {
                        "name": "scenes",
                        "isFile": false,
                        "children": [
                            {
                                "name": "Acorn.js",
                                "isFile": true,
                                "contentType": "js"
                            },
                            {
                                "name": "Ant.js",
                                "isFile": true,
                                "contentType": "js"
                            },
                            {
                                "name": "EnemyDeath.js",
                                "isFile": true,
                                "contentType": "js"
                            },
                            {
                                "name": "GameOver.js",
                                "isFile": true,
                                "contentType": "js"
                            },
                            {
                                "name": "GameOver.scene",
                                "isFile": true,
                                "contentType": "phasereditor2d.scene"
                            },
                            {
                                "name": "Gator.js",
                                "isFile": true,
                                "contentType": "js"
                            },
                            {
                                "name": "Grasshopper.js",
                                "isFile": true,
                                "contentType": "js"
                            },
                            {
                                "name": "Level.js",
                                "isFile": true,
                                "contentType": "js"
                            },
                            {
                                "name": "Level.scene",
                                "isFile": true,
                                "contentType": "phasereditor2d.scene"
                            },
                            {
                                "name": "Player.js",
                                "isFile": true,
                                "contentType": "js"
                            },
                            {
                                "name": "Preload.js",
                                "isFile": true,
                                "contentType": "js"
                            },
                            {
                                "name": "Preload.scene",
                                "isFile": true,
                                "contentType": "phasereditor2d.scene"
                            },
                            {
                                "name": "TitleScreen.js",
                                "isFile": true,
                                "contentType": "js"
                            },
                            {
                                "name": "TitleScreen.scene",
                                "isFile": true,
                                "contentType": "phasereditor2d.scene"
                            }
                        ]
                    },
                    {
                        "name": "sounds",
                        "isFile": false,
                        "children": [
                            {
                                "name": ".DS_Store",
                                "isFile": true,
                                "contentType": "any"
                            },
                            {
                                "name": "enemy-death.ogg",
                                "isFile": true,
                                "contentType": "sound"
                            },
                            {
                                "name": "hurt.ogg",
                                "isFile": true,
                                "contentType": "sound"
                            },
                            {
                                "name": "item.ogg",
                                "isFile": true,
                                "contentType": "sound"
                            },
                            {
                                "name": "jump.ogg",
                                "isFile": true,
                                "contentType": "sound"
                            },
                            {
                                "name": "music-credits.txt",
                                "isFile": true,
                                "contentType": "txt"
                            },
                            {
                                "name": "the_valley.ogg",
                                "isFile": true,
                                "contentType": "sound"
                            }
                        ]
                    },
                    {
                        "name": "sprites",
                        "isFile": false,
                        "children": [
                            {
                                "name": ".DS_Store",
                                "isFile": true,
                                "contentType": "any"
                            },
                            {
                                "name": "credits-text.png",
                                "isFile": true,
                                "contentType": "img"
                            },
                            {
                                "name": "game-over.png",
                                "isFile": true,
                                "contentType": "img"
                            },
                            {
                                "name": "instructions.png",
                                "isFile": true,
                                "contentType": "img"
                            },
                            {
                                "name": "loading.png",
                                "isFile": true,
                                "contentType": "img"
                            },
                            {
                                "name": "press-enter-text.png",
                                "isFile": true,
                                "contentType": "img"
                            },
                            {
                                "name": "title-screen.png",
                                "isFile": true,
                                "contentType": "img"
                            }
                        ]
                    },
                    {
                        "name": "svg",
                        "isFile": false,
                        "children": [
                            {
                                "name": "demo.svg",
                                "isFile": true,
                                "contentType": "img"
                            }
                        ]
                    }
                ]
            },
            {
                "name": "data.json",
                "isFile": true,
                "contentType": "json"
            },
            {
                "name": "fake-assets",
                "isFile": false,
                "children": [
                    {
                        "name": "Collisions Layer.png",
                        "isFile": true,
                        "contentType": "img"
                    },
                    {
                        "name": "Main Layer.png",
                        "isFile": true,
                        "contentType": "img"
                    },
                    {
                        "name": "fake-pack.json",
                        "isFile": true,
                        "contentType": "json"
                    }
                ]
            },
            {
                "name": "index.html",
                "isFile": true,
                "contentType": "any"
            },
            {
                "name": "jsconfig.json",
                "isFile": true,
                "contentType": "json"
            },
            {
                "name": "lib",
                "isFile": false,
                "children": [
                    {
                        "name": "phaser.js",
                        "isFile": true,
                        "contentType": "js"
                    }
                ]
            },
            {
                "name": "main.js",
                "isFile": true,
                "contentType": "js"
            },
            {
                "name": "typings",
                "isFile": false,
                "children": [
                    {
                        "name": "phaser.d.ts",
                        "isFile": true,
                        "contentType": "ts"
                    }
                ]
            }
        ]
    }
        ;

}