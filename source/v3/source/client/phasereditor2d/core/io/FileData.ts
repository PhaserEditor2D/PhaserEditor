namespace phasereditor2d.core.io {
   
    export declare type FileData = {
        name: string;
        isFile: boolean;
        size: number,
        modTime: number,
        children?: FileData[];
    }

}