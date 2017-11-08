package Logica;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.Normalizer;
import java.util.regex.Pattern;

import Snowball.ext.spanishStemmer;
import Snowball.SnowballStemmer;
import org.jsoup.Jsoup;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class Indexer {

    private IndexWriter writer;
    private SnowballStemmer stemmer = new spanishStemmer();
    private static final String stopwords = "(a|acá|ahí|ajena|ajenas|ajeno|ajenos|al|algo|algún|alguna|algunas|alguno|algunos|allá|alli|allí|ambos|ampleamos" +
            "|ante|antes|aquel|aquella|aquellas|aquello|aquellos|aqui|aquí|arriba|asi|atras|aun|aunque|bajo|bastante|bien|cabe|cada|casi|cierta|ciertas" +
            "|cierto|ciertos|como|cómo|con|conmigo|conseguimos|conseguir|consigo|consigue|consiguen|consigues|contigo|contra|cual|cuales|cualquier" +
            "|cualquiera|cualquieras|cuan|cuán|cuando|cuanta|cuánta|cuantas|cuántas|cuanto|cuánto|cuantos|cuántos|de|dejar|del|demás|demas|demasiada" +
            "|demasiadas|demasiado|demasiados|dentro|desde|donde|dos|el|él|ella|ellas|ello|ellos|empleais|emplean|emplear|empleas|empleo|en|encima" +
            "|entonces|entre|era|eramos|eran|eras|eres|es|esa|esas|ese|eso|esos|esta|estaba|estado|estais|estamos|estan|estar|estas|este|esto|estos" +
            "|estoy|etc|fin|fue|fueron|fui|fuimos|gueno|ha|hace|haceis|hacemos|hacen|hacer|haces|hacia|hago|hasta|incluso|intenta|intentais|intentamos" +
            "|intentan|intentar|intentas|intento|ir|jamás|junto|juntos|la|largo|las|lo|los|mas|más|me|menos|mi|mía|mia|mias|mientras|mio|mío|mios|mis" +
            "|misma|mismas|mismo|mismos|modo|mucha|muchas|muchísima|muchísimas|muchísimo|muchísimos|mucho|muchos|muy|nada|ni|ningun|ninguna|ningunas" +
            "|ninguno|ningunos|no|nos|nosotras|nosotros|nuestra|nuestras|nuestro|nuestros|nunca|os|otra|otras|otro|otros|para|parecer|pero|poca|pocas" +
            "|poco|pocos|podeis|podemos|poder|podria|podriais|podriamos|podrian|podrias|por|por qué|porque|primero|primero desde|puede|pueden|puedo|pues" +
            "|que|qué|querer|quien|quién|quienes|quienesquiera|quienquiera|quiza|quizas|sabe|sabeis|sabemos|saben|saber|sabes|se|segun|ser|si|sí|siempre" +
            "|siendo|sin|sín|sino|so|sobre|sois|solamente|solo|somos|soy|sr|sra|sres|sta|su|sus|suya|suyas|suyo|suyos|tal|tales|también|tambien|tampoco" +
            "|tan|tanta|tantas|tanto|tantos|te|teneis|tenemos|tener|tengo|ti|tiempo|tiene|tienen|toda|todas|todo|todos|tomar|trabaja|trabajais|trabajamos" +
            "|trabajan|trabajar|trabajas|trabajo|tras|tú|tu|tus|tuya|tuyo|tuyos|ultimo|un|una|unas|uno|unos|usa|usais|usamos|usan|usar|usas|uso|usted" +
            "|ustedes|va|vais|valor|vamos|van|varias|varios|vaya|verdad|verdadera|vosotras|vosotros|voy|vuestra|vuestras|vuestro|vuestros|y|ya|yo)";
    private Pattern stop = Pattern.compile(stopwords);

    public Indexer(String indexDirectoryPath, boolean create) throws IOException {
        Directory indexDirectory = FSDirectory.open(new File(indexDirectoryPath));
        writer = new IndexWriter(indexDirectory, new StandardAnalyzer(Version.LUCENE_36),create, IndexWriter.MaxFieldLength.UNLIMITED);
    }

    public void close() throws CorruptIndexException, IOException {
        writer.close();
    }

    private Document getDocument(File file) throws IOException {
        org.jsoup.nodes.Document doc = Jsoup.parse(file, "UTF-8");
        String fieldRef = normalizar(doc.select("a[href]").text()
                .replaceAll("\\s+", " ")
                .replaceAll("[^a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]+", ""));
        String[] fieldParList = doc.select("p").text()
                .replaceAll("\\s+", " ")
                .replaceAll("[^a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]+", "")
                .split(" ");
        String fieldPar = "";
        for (String s : fieldParList) {
            if(!stop.matcher(s).matches()) {
                stemmer.setCurrent(s.toLowerCase());
                stemmer.stem();
                fieldPar += stemmer.getCurrent() + " ";
            }
        }
        Document document = new Document();
        Field refField = new Field(LuceneConstants.REF, fieldRef, Field.Store.YES, Field.Index.ANALYZED);
        Field parField = new Field(LuceneConstants.PAR, fieldPar, Field.Store.YES, Field.Index.ANALYZED);
        Field fileNameField = new Field(LuceneConstants.FILE_NAME, file.getName(), Field.Store.YES, Field.Index.NOT_ANALYZED);
        Field filePathField = new Field(LuceneConstants.FILE_PATH, file.getCanonicalPath(), Field.Store.YES, Field.Index.NOT_ANALYZED);
        document.add(refField);
        document.add(parField);
        document.add(fileNameField);
        document.add(filePathField);
        return document;
    }

    private void indexFile(File file) throws IOException {
        System.out.println("Indexing "+file.getName());
        Document document = getDocument(file);
        writer.addDocument(document);
    }

    public int createIndex(String dataDirPath, FileFilter filter) throws IOException {
        File file = new File(dataDirPath);
        if(!file.isDirectory() && !file.isHidden() && file.exists() && file.canRead() && filter.accept(file)){
            indexFile(file);
        }
        return writer.numDocs();
    }

    public String normalizar(String string)
    {
        string = string.toLowerCase();
        string = string.replace('ñ', '\001');
        string = Normalizer.normalize(string, Normalizer.Form.NFD);
        string = string.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        string = string.replace('\001', 'ñ');
        return string;
    }
}
