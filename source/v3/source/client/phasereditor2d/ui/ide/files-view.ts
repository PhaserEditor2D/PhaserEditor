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

        getImage(obj: any): HTMLImageElement {
            const file = <io.FilePath>obj;
            let icon = file.isFile() ? controls.Controls.ICON_FILE : controls.Controls.ICON_FOLDER;
            return controls.Controls.getIcon(icon);
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

            let root = new core.io.FilePath(null, TEST_DATA);

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

    const TEST_DATA = {
        "name": "",
        "isFile": false,
        "children": [
            {
                "name": ".gitignore",
                "isFile": true
            },
            {
                "name": "COPYRIGHTS",
                "isFile": true
            },
            {
                "name": "assets",
                "isFile": false,
                "children": [
                    {
                        "name": "animations.json",
                        "isFile": true
                    },
                    {
                        "name": "atlas",
                        "isFile": false,
                        "children": [
                            {
                                "name": ".DS_Store",
                                "isFile": true
                            },
                            {
                                "name": "atlas-props.json",
                                "isFile": true
                            },
                            {
                                "name": "atlas-props.png",
                                "isFile": true
                            },
                            {
                                "name": "atlas.json",
                                "isFile": true
                            },
                            {
                                "name": "atlas.png",
                                "isFile": true
                            },
                            {
                                "name": "hello.atlas",
                                "isFile": true
                            },
                            {
                                "name": "hello.json",
                                "isFile": true
                            },
                            {
                                "name": "hello.png",
                                "isFile": true
                            }
                        ]
                    },
                    {
                        "name": "environment",
                        "isFile": false,
                        "children": [
                            {
                                "name": ".DS_Store",
                                "isFile": true
                            },
                            {
                                "name": "bg-clouds.png",
                                "isFile": true
                            },
                            {
                                "name": "bg-mountains.png",
                                "isFile": true
                            },
                            {
                                "name": "bg-trees.png",
                                "isFile": true
                            },
                            {
                                "name": "tileset.png",
                                "isFile": true
                            }
                        ]
                    },
                    {
                        "name": "fonts",
                        "isFile": false,
                        "children": [
                            {
                                "name": "arcade.png",
                                "isFile": true
                            },
                            {
                                "name": "arcade.xml",
                                "isFile": true
                            },
                            {
                                "name": "atari-classic.png",
                                "isFile": true
                            },
                            {
                                "name": "atari-classic.xml",
                                "isFile": true
                            }
                        ]
                    },
                    {
                        "name": "html",
                        "isFile": false,
                        "children": [
                            {
                                "name": "hello.html",
                                "isFile": true
                            }
                        ]
                    },
                    {
                        "name": "levels-pack-1.json",
                        "isFile": true
                    },
                    {
                        "name": "maps",
                        "isFile": false,
                        "children": [
                            {
                                "name": ".DS_Store",
                                "isFile": true
                            },
                            {
                                "name": "map.json",
                                "isFile": true
                            }
                        ]
                    },
                    {
                        "name": "scenes",
                        "isFile": false,
                        "children": [
                            {
                                "name": "Acorn.js",
                                "isFile": true
                            },
                            {
                                "name": "Ant.js",
                                "isFile": true
                            },
                            {
                                "name": "EnemyDeath.js",
                                "isFile": true
                            },
                            {
                                "name": "GameOver.js",
                                "isFile": true
                            },
                            {
                                "name": "GameOver.scene",
                                "isFile": true
                            },
                            {
                                "name": "Gator.js",
                                "isFile": true
                            },
                            {
                                "name": "Grasshopper.js",
                                "isFile": true
                            },
                            {
                                "name": "Level.js",
                                "isFile": true
                            },
                            {
                                "name": "Level.scene",
                                "isFile": true
                            },
                            {
                                "name": "Player.js",
                                "isFile": true
                            },
                            {
                                "name": "Preload.js",
                                "isFile": true
                            },
                            {
                                "name": "Preload.scene",
                                "isFile": true
                            },
                            {
                                "name": "TitleScreen.js",
                                "isFile": true
                            },
                            {
                                "name": "TitleScreen.scene",
                                "isFile": true
                            }
                        ]
                    },
                    {
                        "name": "sounds",
                        "isFile": false,
                        "children": [
                            {
                                "name": ".DS_Store",
                                "isFile": true
                            },
                            {
                                "name": "enemy-death.ogg",
                                "isFile": true
                            },
                            {
                                "name": "hurt.ogg",
                                "isFile": true
                            },
                            {
                                "name": "item.ogg",
                                "isFile": true
                            },
                            {
                                "name": "jump.ogg",
                                "isFile": true
                            },
                            {
                                "name": "music-credits.txt",
                                "isFile": true
                            },
                            {
                                "name": "the_valley.ogg",
                                "isFile": true
                            }
                        ]
                    },
                    {
                        "name": "sprites",
                        "isFile": false,
                        "children": [
                            {
                                "name": ".DS_Store",
                                "isFile": true
                            },
                            {
                                "name": "credits-text.png",
                                "isFile": true
                            },
                            {
                                "name": "game-over.png",
                                "isFile": true
                            },
                            {
                                "name": "instructions.png",
                                "isFile": true
                            },
                            {
                                "name": "loading.png",
                                "isFile": true
                            },
                            {
                                "name": "press-enter-text.png",
                                "isFile": true
                            },
                            {
                                "name": "title-screen.png",
                                "isFile": true
                            }
                        ]
                    },
                    {
                        "name": "svg",
                        "isFile": false,
                        "children": [
                            {
                                "name": "demo.svg",
                                "isFile": true
                            }
                        ]
                    }
                ]
            },
            {
                "name": "data.json",
                "isFile": true
            },
            {
                "name": "fake-assets",
                "isFile": false,
                "children": [
                    {
                        "name": "Collisions Layer.png",
                        "isFile": true
                    },
                    {
                        "name": "Main Layer.png",
                        "isFile": true
                    },
                    {
                        "name": "fake-pack.json",
                        "isFile": true
                    }
                ]
            },
            {
                "name": "index.html",
                "isFile": true
            },
            {
                "name": "jsconfig.json",
                "isFile": true
            },
            {
                "name": "lib",
                "isFile": false,
                "children": [
                    {
                        "name": "phaser.js",
                        "isFile": true
                    }
                ]
            },
            {
                "name": "main.js",
                "isFile": true
            },
            {
                "name": "typings",
                "isFile": false,
                "children": [
                    {
                        "name": "phaser.d.ts",
                        "isFile": true
                    }
                ]
            }
        ]
    };

}