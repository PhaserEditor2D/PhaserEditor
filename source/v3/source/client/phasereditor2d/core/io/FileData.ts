namespace phasereditor2d.core.io {
   
    export declare type FileData = {
        name: string;
        isFile: boolean;
        children?: FileData[];
        contentType?: string;
    }

}