namespace phasereditor2d.core {

    export const CONTENT_TYPE_ANY = "any";

    export interface IContentTypeResolver {
        computeContentType(file : io.FilePath): Promise<string>;
    }
}