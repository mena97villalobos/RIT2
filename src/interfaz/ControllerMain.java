package interfaz;

import Logica.*;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTreeCell;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.jsoup.Jsoup;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

public class ControllerMain implements Initializable{
    @FXML
    private Button loadPath;
    @FXML
    private TextField pathField;
    @FXML
    private TextField consultaText;
    @FXML
    private TreeView treeView;
    @FXML
    private TextArea Resultados;
    @FXML
    private Button indexar;
    @FXML
    private Button consultar;
    @FXML
    private Label label;

    private ArrayList<String> checkedItems = new ArrayList<>();
    private String consulta = "";
    public Searcher searcher;
    public boolean create = true;
    public Set<String> stopWords;

    public ControllerMain() throws IOException {
        stopWords = new HashSet<String>(Files.readAllLines(Paths.get("stopWords.txt")));
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        File dir = new File("Salida");
        for (File file: dir.listFiles()) if (!file.isDirectory()) file.delete();
        consultar.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                Resultados.clear();
                consulta = consultaText.getText();
                if(consulta.equals("")){
                    Resultados.setText("Consulta nula!!!!!");
                }
                else {
                    String result = "";
                    try {
                        result = search(consulta, "Salida");
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    Resultados.setText(result);
                }
            }
        });
        loadPath.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if(!pathField.getText().isEmpty()) {
                    label.setText("");
                    displayTreeView(pathField.getText());
                }
                else
                    label.setText("Ingrese un path para cargar!!!");
            }
        });
        indexar.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                Task task = new Task() {
                    @Override
                    protected Void call() throws Exception {
                        int numIndexed;
                        long startTime = System.currentTimeMillis();
                        this.updateMessage("Indexando...");
                        String aux;
                        checkedItems.clear();
                        findCheckedItems((CheckBoxTreeItem<?>) treeView.getRoot(), checkedItems);
                        if(checkedItems.isEmpty()){
                            this.updateMessage("Seleccione archivos para indexar!!!");
                        }
                        else {
                            Directory indexDirectory = FSDirectory.open(Paths.get("Salida"));

                            SpanishAnalyzer spanishAnalyzer = new SpanishAnalyzer(CharArraySet.copy(stopWords));
                            IndexWriterConfig config = new IndexWriterConfig(spanishAnalyzer);
                            if (create)
                                config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
                            else
                                config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
                            IndexWriter writer = new IndexWriter(indexDirectory, config);
                            for (String s : checkedItems) {
                                this.updateMessage("Indexando: " + s);
                                indexFile(new File(s), writer, new TextFileFilter());
                            }
                            numIndexed = writer.numDocs();
                            writer.close();
                            long endTime = System.currentTimeMillis();
                            aux = numIndexed + " Archivos indexados, duración: " + (endTime - startTime) + " ms";
                            this.updateMessage(aux);
                            this.updateMessage(this.workDoneProperty().toString());
                            create = false;
                        }
                        return null;
                    }
                };
                Thread t = new Thread(task);
                label.textProperty().bind(task.messageProperty());
                t.start();
            }
        });
        displayTreeView("Geografia");
    }

    private void findCheckedItems(CheckBoxTreeItem<?> item, ArrayList<String> checkedItems) {
        TextFileFilter filter = new TextFileFilter();
        if (item.isSelected()) {
            File path = new File(item.getValue().toString());
            if (filter.accept(path))
                checkedItems.add(item.getValue().toString());
        }
        for (TreeItem<?> child : item.getChildren()) {
            findCheckedItems((CheckBoxTreeItem<?>) child, checkedItems);
        }
    }

    public static void createTree(File file, CheckBoxTreeItem<String> parent) {
        if (file.isDirectory()) {
            CheckBoxTreeItem<String> treeItem = new CheckBoxTreeItem<>(file.getName());
            parent.getChildren().add(treeItem);
            for (File f : file.listFiles()) {
                createTree(f, treeItem);
            }
        }
        else {
            parent.getChildren().add(new CheckBoxTreeItem<>(file.getPath()));
        }
    }

    public void displayTreeView(String inputDirectoryLocation) {
        CheckBoxTreeItem<String> rootItem = new CheckBoxTreeItem<>(inputDirectoryLocation);
        treeView.setShowRoot(false);
        treeView.setCellFactory(CheckBoxTreeCell.<String>forTreeView());
        File fileInputDirectoryLocation = new File(inputDirectoryLocation);
        File fileList[] = fileInputDirectoryLocation.listFiles();
        for (File file : fileList) {
            createTree(file, rootItem);
        }
        treeView.setRoot(rootItem);
    }

    private String search(String searchQuery, String indexDir) throws IOException, ParseException {
        String similitud;
        String nombre;
        String result = "";
        int cont = 1;
        searcher = new Searcher();
        TopDocs hits = searcher.search(searchQuery);
        for(ScoreDoc scoreDoc : hits.scoreDocs) {
            Document doc = searcher.getDocument(scoreDoc);
            similitud = Float.toString(scoreDoc.score);
            nombre = doc.get(LuceneConstants.FILE_NAME);
            result += Integer.toString(cont) + "  Nombre: " + nombre + " Similitud: " + similitud + "\n";
            cont++;
        }
        result += "-----------------------------------------------------------------------\n";
        result += Float.toString(hits.totalHits) + " documentos encontrados.";
        return result;
    }

    private void indexFile(File file, IndexWriter writer, TextFileFilter filter) throws IOException {
        if(!file.isDirectory() && !file.isHidden() && file.exists() && file.canRead() && filter.accept(file)) {
            org.jsoup.nodes.Document doc = Jsoup.parse(file, "UTF-8");
            String fieldRef = normalizar(doc.select("a[href]").text()
                    .replaceAll("\\s+", " ")
                    .replaceAll("[^a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]+", ""));
            String fieldPar = doc.select("p").text()
                    .replaceAll("\\s+", " ")
                    .replaceAll("[^a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]+", "");
            fieldRef = normalizar(fieldRef);
            fieldPar = normalizar(fieldPar);
            Document document = new Document();
            org.apache.lucene.document.TextField refField = new org.apache.lucene.document.TextField(LuceneConstants.REF, fieldRef, Field.Store.YES);
            org.apache.lucene.document.TextField parField = new org.apache.lucene.document.TextField(LuceneConstants.PAR, fieldPar, Field.Store.YES);
            StringField fileNameField = new StringField(LuceneConstants.FILE_NAME, file.getName(), Field.Store.YES);
            StringField filePathField = new StringField(LuceneConstants.FILE_PATH, file.getCanonicalPath(), Field.Store.YES);
            document.add(refField);
            document.add(parField);
            document.add(fileNameField);
            document.add(filePathField);
            if (writer.getConfig().getOpenMode() == IndexWriterConfig.OpenMode.CREATE) {
                writer.addDocument(document);
            } else {
                writer.updateDocument(new Term("name", file.getName()), document);
            }
        }
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
