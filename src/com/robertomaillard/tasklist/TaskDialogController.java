package com.robertomaillard.tasklist;

import com.robertomaillard.tasklist.datamodel.TaskData;
import com.robertomaillard.tasklist.datamodel.TaskItem;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.time.LocalDate;

/**
 * Created by Roberto Maillard on 17/9/2019.
 * Dialog FXML controller Class.
 */

public class TaskDialogController {

    @FXML
    private TextField shortDescriptionField;

    @FXML
    private TextArea detailsArea;

    @FXML
    private DatePicker deadlinePicker;


    public TaskItem processResult() {
        String shortDescription = shortDescriptionField.getText().trim();
        String details = detailsArea.getText().trim();
        LocalDate deadlineValue = deadlinePicker.getValue();

        if(deadlineValue==null){
            deadlineValue=LocalDate.now();
        }

        TaskItem newItem = new TaskItem(shortDescription, details, deadlineValue);
        TaskData.getInstance().addTaskItem(newItem);

        // ListView SELECTS THE RETURN OBJECT
        return newItem;
    }

    // POPULATES THE EDIT DIALOG WITH TASK ITEM DATA FROM THE TARGET TASK ITEM
    void setDataToForm(TaskItem item) {
        shortDescriptionField.setText(item.getShortDescription());
        detailsArea.setText(item.getDetails());
        deadlinePicker.setValue(item.getDeadline());
    }

//    MODIFIES THE TARGET TASK ITEM
    void modifyItem(TaskItem item) {
        String shortDesc = shortDescriptionField.getText().trim();
        String detailsDesc = detailsArea.getText().trim();
        LocalDate deadlineValue = deadlinePicker.getValue();

        if(deadlineValue==null){
            deadlineValue=LocalDate.now();
        }

        TaskData.getInstance().editTaskItem(item, new TaskItem(shortDesc, detailsDesc, deadlineValue));
    }
}
