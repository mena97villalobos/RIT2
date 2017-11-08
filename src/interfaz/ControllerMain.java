package interfaz;

import Logica.*;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTreeCell;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

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
    public Indexer indexer = new Indexer("Salida", true);
    public Searcher searcher;
    private Pattern caso3 = Pattern.compile("(ref:.+|.) AND (ref:.+|.)");
    private Pattern caso6 = Pattern.compile("\\d\\.\\d");

    public ControllerMain() throws IOException { }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        File dir = new File("Salida");
        for (File file: dir.listFiles()) if (!file.isDirectory()) file.delete();
        consultar.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if(indexer != null) {
                    try {
                        indexer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
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
                        int numIndexed = 0;
                        long startTime = System.currentTimeMillis();
                        this.updateMessage("Indexando...");
                        String aux = "";
                        checkedItems.clear();
                        findCheckedItems((CheckBoxTreeItem<?>) treeView.getRoot(), checkedItems);
                        try {
                            if(indexer == null){
                                indexer = new Indexer("Salida", false);
                            }
                            for (String s : checkedItems) {
                                this.updateMessage("Indexando: "+s);
                                numIndexed = indexer.createIndex(s, new TextFileFilter());
                            }
                            indexer.close();
                            indexer = null;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        long endTime = System.currentTimeMillis();
                        aux = numIndexed+" Archivos indexados, duración: " +(endTime-startTime)+" ms";
                        this.updateMessage(aux);
                        this.updateMessage(this.workDoneProperty().toString());
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
        String similitud = "";
        String nombre = "";
        String result = "";
        int cont = 1;
        searcher = new Searcher(indexDir);
        long startTime = System.currentTimeMillis();
        TopDocs hits = searcher.search(searchQuery);
        long endTime = System.currentTimeMillis();
        for(ScoreDoc scoreDoc : hits.scoreDocs) {
            Document doc = searcher.getDocument(scoreDoc);
            similitud = Float.toString(scoreDoc.score);
            nombre = doc.get(LuceneConstants.FILE_NAME);
            result += Integer.toString(cont) + "  Nombre: " + nombre + " Similitud: " + similitud + "\n";
            cont++;
        }
        result += "-----------------------------------------------------------------------\n";
        result += Integer.toString(hits.totalHits) + " documentos encontrados.";
        return result;
    }
}
