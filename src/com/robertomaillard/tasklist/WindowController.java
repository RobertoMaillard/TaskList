package com.robertomaillard.tasklist;

import com.robertomaillard.tasklist.datamodel.TaskData;
import com.robertomaillard.tasklist.datamodel.TaskItem;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.util.Callback;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Created by Roberto Maillard on 19/9/2019.
 * Window FXML controller Class.
 */

public class WindowController {

    @FXML
    private ListView<TaskItem> taskListView;

    @FXML
    private TextArea itemDetailsTextArea;

    @FXML
    private Label deadLineLabel;

    @FXML
    private BorderPane mainBorderPane;

    @FXML
    private ContextMenu listContextMenu;

    @FXML
    private ToggleButton filterToggleButton;


    private FilteredList<TaskItem> filteredList;

    private Predicate<TaskItem> wantAllItems;
    private Predicate<TaskItem> wantTodaysItems;

    public void initialize() {

        // CREATES A PREDICATE TO PASS TO FilteredList
        wantAllItems = new Predicate<TaskItem>() {
            @Override
            public boolean test(TaskItem todoItem) {
                // LETS ALL PASS
                return true;
            }
        };

        // CREATES A PREDICATE TO PASS TO FilteredList
        wantTodaysItems = new Predicate<TaskItem>() {
            @Override
            public boolean test(TaskItem todoItem) {
                // LETS ONLY DEADLINE TODAY PASS
                return (todoItem.getDeadline().equals(LocalDate.now()));
            }
        };

//        RETRIEVES AN ObservableList POPULATED WITH TASK ITEMS FROM THE DATABASE
        ObservableList<TaskItem> taskItems = TaskData.getInstance().getTaskItems();

//        IF TASKLIST IS EMPTY. ADD SOME INFORMATIVE INITIAL TASK ITEMS
        if(taskItems.isEmpty()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            TaskItem previousTask =
                    new TaskItem("Previous task",
                            "Previous task are marked in light gray", LocalDate.parse("01-01-2019", formatter));
            TaskItem todaysTask = new TaskItem("Today's task",
                    "Today's task are marked in light gray", LocalDate.now());
            TaskItem tomorrowsTask = new TaskItem("Tomorrow's task",
                    "Tomorrow's task are marked in orange", LocalDate.now().plusDays(1));
            TaskItem futureTask = new TaskItem("Future task",
                    "Future task is not marked", LocalDate.parse("01-01-2029", formatter));

            TaskData.getInstance().addTaskItem(previousTask);
            TaskData.getInstance().addTaskItem(todaysTask);
            TaskData.getInstance().addTaskItem(tomorrowsTask);
            TaskData.getInstance().addTaskItem(futureTask);
        }

//        WRAPS THE ObservableList AND FILTER'S IT'S CONTENT USING THE PROVIDED PREDICATE
        filteredList = new FilteredList<TaskItem>(taskItems, wantAllItems);

//        WRAPS A FilteredList AND SORT'S IT'S CONTENT BY IMPLEMENTING COMPARATOR COMPARE METHOD
        SortedList<TaskItem> sortedList = new SortedList<TaskItem>(filteredList,
                new Comparator<TaskItem>() {
                    @Override
                    public int compare(TaskItem o1, TaskItem o2) {

//                        COMPARES TaskItem object 1 TO TodoItem object 2
                        return o1.getDeadline().compareTo(o2.getDeadline());
                    }
                });

//        POPULATES THE FXML ListView WITH THE SortedList
        taskListView.setItems(sortedList);

//        ADDS A ChangeListener TO THE ListView LIST
        taskListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TaskItem>() {

            //ChangeListener IS NOTIFIED WHENEVER THE VALUE OF AN ObservableValue CHANGES
            @Override
            public void changed(ObservableValue<? extends TaskItem> observableValue, TaskItem todoItem, TaskItem t1) {
                if (t1 != null) {

                    // SELECTED TASK ITEM IS DISPLAYED IN DETAILS TextArea AND DEADLINES Label
                    TaskItem item = taskListView.getSelectionModel().getSelectedItem();
                    itemDetailsTextArea.setText(item.getDetails());

                    // FORMATS DATE
                    DateTimeFormatter df = DateTimeFormatter.ofPattern("cccc d MMMM, yyyy");
                    deadLineLabel.setText(df.format(item.getDeadline()));
                }
            }
        });

//        SETS SELECTION MODE TO SINGLE
        taskListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

//        ITERATE THROUGH SortedList TO SELECT APPROPRIATE TASK ITEM
        for(TaskItem taskItem : sortedList) {
            if (taskItem.getDeadline().equals(LocalDate.now())) {
                taskListView.getSelectionModel().select(taskItem);
                break;
            } else if(taskItem.getDeadline().isAfter(LocalDate.now())) {
                taskListView.getSelectionModel().select(taskItem);
                break;
            } else {
                //SELECT'S THE FIRST INDEX
                taskListView.getSelectionModel().selectFirst();
            }
        }

//        CONTEXT MENU HANDLER
        listContextMenu = new ContextMenu();

//        DELETE ITEM VIA LISTVIEW CONTEXT MENU
        MenuItem deleteMenuItem = new MenuItem("Delete item...");
        deleteMenuItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                TaskItem todoItem = taskListView.getSelectionModel().getSelectedItem();
                deleteItem(todoItem);
            }
        });

//        EDITS ITEM VIA LISTVIEW CONTEXT MENU
        MenuItem editMenuItem = new MenuItem("Edit item...");
        editMenuItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                TaskItem todoItem = taskListView.getSelectionModel().getSelectedItem();
                modifyItem(todoItem);
            }
        });

        // POPULATES FXML LISTVIEW CONTEXT MENU
        listContextMenu.getItems().addAll(deleteMenuItem,editMenuItem);

//        SETS A CellFactory
        taskListView.setCellFactory(new Callback<ListView<TaskItem>, ListCell<TaskItem>>() {
            @Override
            public ListCell<TaskItem> call(ListView<TaskItem> taskItemListView) {
                ListCell<TaskItem> cell = new ListCell<>() {

                    // UPDATES ListView CELL COLORS
                    @Override
                    protected void updateItem(TaskItem taskItem, boolean b) {
                        super.updateItem(taskItem, b);
                        if (b) {
                            setText(null);
                        } else {
                            setText(taskItem.getShortDescription());
                            if (taskItem.getDeadline().isBefore(LocalDate.now())) {
                                // UPDATES PAST DATE'S CELL COLOR TO GREY
                                setTextFill(Color.GRAY);
                            }
                            else if (taskItem.getDeadline().equals(LocalDate.now())) {
                                // UPDATES TODAY'S DUE DATE CELL'S COLOR TO RED
                                setTextFill(Color.RED);
                            } else if (taskItem.getDeadline().equals(LocalDate.now().plusDays(1))) {
                                // UPDATES TOMORROW'S DUE DATE CELL'S COLOR TO ORANGE
                                setTextFill(Color.ORANGE);
                            }
                        }
                    }
                };

//                LAMBDA EXPRESSION. ASSOCIATE CELL WITH CONTEXT MENU WHEN CELL LIST IS NOT EMPTY
                cell.emptyProperty().addListener(
                        (obs, wasEmpty, isNowEmpty) -> {
                            if(isNowEmpty) {
                                cell.setContextMenu(null);
                            } else {
                                cell.setContextMenu(listContextMenu);
                            }
                        }

                );

                return cell;
            }
        });
    }


//    SHOW NEW ITEM DIALOG
    @FXML
    public void showNewItemDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(mainBorderPane.getScene().getWindow());
        dialog.setTitle("Add New Task");
        dialog.setHeaderText("Use this dialog to create a new task item");

        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(getClass().getResource("taskDialog.fxml"));
        try {
            dialog.getDialogPane().setContent(fxmlLoader.load());
        } catch(IOException e){
            System.out.println("Could not load the dialog");
            e.printStackTrace();
            return;
        }

        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            TaskDialogController controller = fxmlLoader.getController();
            TaskItem newItem = controller.processResult();
            taskListView.getSelectionModel().select(newItem);
        }
    }

//    FILTER TODAY'S DUE DATE ITEM'S BY SETTING APPROPRIATE PREDICATE
    @FXML
    public void handleFilterButton() {
        if(filterToggleButton.isSelected()) {
            filteredList.setPredicate(wantTodaysItems);
        } else {
            filteredList.setPredicate(wantAllItems);
        }
    }

//    HANDLES PRESSED KEY WHILE TASK ITEM SELECTED
    @FXML
    public void handleKeyPressed(KeyEvent keyEvent) {
        TaskItem selectedItem = taskListView.getSelectionModel().getSelectedItem();

        // DELETE ITEMS FROM ITEMS LIST BY PRESSING DELETE KEY
        if(selectedItem != null) {
            if(keyEvent.getCode().equals(KeyCode.DELETE)) {
                deleteItem(selectedItem);
            }
        }
    }

//    DELETE ITEMS FROM ITEMS LIST BY RIGHT CLICK
    public void deleteItem(TaskItem taskItem) {
        // ALERT CONFIRMATION DIALOG
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Item");
        alert.setHeaderText("Delete item: " + taskItem.getShortDescription());
        alert.setContentText("Are you sure? Press OK to confirm, or Cancel to cancel");
        Optional<ButtonType> result = alert.showAndWait();

        // IF RESULT IS PRESENT AND OK BUTTON IS PRESSED
        if (result.isPresent() && (result.get() == ButtonType.OK)) {
            TaskData.getInstance().deleteTaskItem(taskItem);
        }
    }

//    MODIFIES ITEMS FROM LIST BY RIGHT CLICK
    private void modifyItem(TaskItem item) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(mainBorderPane.getScene().getWindow());
        dialog.setTitle("Edit Item");
        dialog.setHeaderText("Edit taskItem: " + item.getShortDescription());

        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(getClass().getResource("taskDialog.fxml"));

        try{
            dialog.getDialogPane().setContent(fxmlLoader.load());
        } catch(IOException error) {
            System.out.println("Couldn't load fxml file!");
            error.printStackTrace();
        }

        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);

        TaskDialogController controller = fxmlLoader.getController();
        controller.setDataToForm(item);

        Optional<ButtonType> result = dialog.showAndWait();
        if(result.isPresent() && result.get() == ButtonType.OK) {
            controller.modifyItem(item);
        }
    }

}
