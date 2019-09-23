package com.robertomaillard.tasklist.datamodel;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.w3c.dom.*;

import javax.xml.parsers.*;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Created by Roberto Maillard on 19/9/2019.
 * Task data singleton Class. Only one instance of this class at a time.
 */

public class TaskData {

//    PRIVATE STATIC INSTANCE OF THIS CLASS
    private static TaskData instance = new TaskData();

    private static final String XML_CONNECTION_STRING = "TaskListItems.xml";

    private static final String DB_NAME = "TaskListItems.db";
    private static final String DB_CONNECTION_STRING = "jdbc:sqlite:" + DB_NAME;

    private ObservableList<TaskItem> taskItems;

    private DateTimeFormatter formatter;

    public enum DataSource {
        DB,
        XML
    }

    DataSource dataSource;

//    RETURNS AN INSTANCE OF THIS SINGLETON CLASS
    public static TaskData getInstance() {
        return instance;
    }

    private TaskData() {
        formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    }

    public ObservableList<TaskItem> getTaskItems() {
        return taskItems;
    }

    public void loadTaskItems(DataSource dataSource) {
        switch(dataSource) {
            case DB:
                loadDBTaskItems();
                this.dataSource = DataSource.DB;
                System.out.println("Task items from the SQLite database are loaded");
                break;

            case XML:
                loadXMLTaskItems();
                this.dataSource = DataSource.XML;
                System.out.println("Task items from the XLM file are loaded");
                break;
        }
    }

//    LOADS TASK ITEMS FROM A XML TO THE ListView's ObservableList
    private void loadXMLTaskItems() {

        // CREATES THE ObservableList
        taskItems = FXCollections.observableArrayList();

        try {
            // CREATES A DocumentBuilder FROM THE XML FILE
            File inputFile = new File(XML_CONNECTION_STRING);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            // CREATES A Document FROM A DocumentBuilder
            Document document = dBuilder.parse(inputFile);
            document.getDocumentElement().normalize();

            // CREATE A NODE LIST OF ELEMENTS BY TAG NAME
            NodeList nList = document.getElementsByTagName("task");

            // ITERATES THROUGH EVERY NODE IN THE NodeList
            for (int i = 0; i < nList.getLength(); i++) {

                Node nNode = nList.item(i);

                if (nNode.getNodeType() == Node.ELEMENT_NODE) {

                    Element eElement = (Element) nNode;

                    String shortDescription = eElement.getElementsByTagName("shortDescription").item(0).getTextContent();
                    String details = eElement.getElementsByTagName("details").item(0).getTextContent();
                    String dateString = eElement.getElementsByTagName("deadline").item(0).getTextContent();

                    LocalDate date = LocalDate.parse(dateString, formatter);
                    TaskItem taskItem = new TaskItem(shortDescription, details, date);
                    taskItems.add(taskItem);
                }

            }

        } catch (Exception e) {
            System.out.println("Couldn't load task items from XML file:  " + e.getMessage());
            e.printStackTrace();
        }
    }

//    LOADS TASK ITEMS FROM A DATABASE TO THE ListView's ObservableList
    private void loadDBTaskItems() {

        // CONNECTS TO THE DATABASE. GETS A CONNECTION INSTANCE
        try(Connection connection = DriverManager.getConnection(DB_CONNECTION_STRING)) {

            // CREATES A STATEMENT INSTANCE
            try(Statement statement = connection.createStatement()) {

                // EXECUTES A CREATE TABLE IF NOT EXISTS STATEMENT
                statement.execute("CREATE TABLE IF NOT EXISTS taskitems " +
                        "(shortDescription TEXT, details TEXT, deadline TEXT)");

                // CREATES A ResultSet RESOURCE
                try(ResultSet resultSet = statement.executeQuery("SELECT * FROM taskitems")) {

                    // CREATES THE ObservableList
                    taskItems = FXCollections.observableArrayList();

                    // POPULATES THE ObservableList
                    while(resultSet.next()) {
                        String shortDescription = resultSet.getString("shortDescription");
                        String details = resultSet.getString("details");
                        String dateString = resultSet.getString("deadline");

                        LocalDate date = LocalDate.parse(dateString, formatter);
                        TaskItem taskItem = new TaskItem(shortDescription, details, date);
                        taskItems.add(taskItem);
                    }

                } catch (SQLException e) {
                    System.out.println("Could create a resultset: " + e.getMessage());
                }

            } catch(SQLException e) {
                System.out.println("Could create a database statement: " + e.getMessage());
            }

        } catch (SQLException e) {
            System.out.println("Could not connect to the database: " + e.getMessage());
        }

    }

    public void  addTaskItem(TaskItem taskItem) {

        switch(this.dataSource) {
            case DB:
                addDBTaskItem(taskItem);
                System.out.println("Task item is added to the SQLite database");
                break;

            case XML:
                addXMLTaskItem(taskItem);
                System.out.println("Task items is added to the XLM file");
                break;
        }
    }

//    ADD TASK ITEM TO XML FILE
    private void addXMLTaskItem(TaskItem taskItem) {

        try {
            // CREATES A DocumentBuilder FROM THE XML FILE
            File inputFile = new File(XML_CONNECTION_STRING);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            // CREATES A Document FROM A DocumentBuilder
            Document document = dBuilder.parse(inputFile);

            String shortDescription = taskItem.getShortDescription();
            String details = taskItem.getDetails();
            String deadline = taskItem.getDeadline().format(formatter);

            // RETURNS THE ROOT ELEMENT OF THE DOCUMENT
            Element rootElement = document.getDocumentElement();

            // APPENDS TASK ELEMENT TO THE ROOT ELEMENT
            Element taskElement = document.createElement("task");
            rootElement.appendChild(taskElement);

            // APPENDS TASK ITEM ELEMENT TO THE TASK ELEMENT
            Element shortDescriptionElement = document.createElement("shortDescription");
            shortDescriptionElement.appendChild(document.createTextNode(shortDescription));
            taskElement.appendChild(shortDescriptionElement);

            Element detailsElement = document.createElement("details");
            detailsElement.appendChild(document.createTextNode(details));
            taskElement.appendChild(detailsElement);

            Element deadlineElement = document.createElement("deadline");
            deadlineElement.appendChild(document.createTextNode(deadline));
            taskElement.appendChild(deadlineElement);

            // WRITES THE CONTENT INTO XML FILE
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(inputFile);
            transformer.transform(source, result);

            // UPDATES THE OBSERVABLE LIST THAT PROVIDES THE FXML ListView
            taskItems.add(taskItem);

        } catch (Exception e) {
            System.out.println("Couldn't add task items to XML file:  " + e.getMessage());
        }

    }

//    ADD TASK ITEM TO DATABASE
    private void addDBTaskItem(TaskItem taskItem) {

        String shortDescription = taskItem.getShortDescription();
        String details = taskItem.getDetails();
        String deadline = taskItem.getDeadline().format(formatter);

        String insertTaskStatement = "INSERT INTO taskitems VALUES (\"" +
                                    shortDescription + "\", \"" +
                                    details + "\", \"" +
                                    deadline + "\")";

        try(Connection connection = DriverManager.getConnection(DB_CONNECTION_STRING);
            PreparedStatement insertTaskitems = connection.prepareStatement(insertTaskStatement)) {

            // EXECUTES INSERT INTO taskitems TABLE
            insertTaskitems.execute();

            // UPDATES THE OBSERVABLE LIST THAT PROVIDES THE FXML ListView
           taskItems.add(taskItem);

        } catch (SQLException e) {
            System.out.println("Couldn't open connection: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void deleteTaskItem(TaskItem taskItem) {

        switch(this.dataSource) {
            case DB:
                deleteDBTaskItem(taskItem);
                System.out.println("Task item is deleted from the SQLite database");
                break;

            case XML:
                deleteXMLTaskItem(taskItem);
                System.out.println("Task items is deleted from the XLM file");
                break;
        }
    }

//    DELETE TASK ITEM FROM XML FILE
    private void deleteXMLTaskItem(TaskItem taskItem) {

        try {
            // CREATES A DocumentBuilder FROM THE XML FILE
            File inputFile = new File(XML_CONNECTION_STRING);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            // CREATES A Document FROM A DocumentBuilder
            Document document = dBuilder.parse(inputFile);

            String shortDescription = taskItem.getShortDescription();

            // RETURNS THE ROOT ELEMENT OF THE DOCUMENT
            Element rootElement = document.getDocumentElement();

            // CREATE A NODE LIST OF ELEMENTS BY TAG NAME
            NodeList shortDescriptionNodeList = document.getElementsByTagName("shortDescription");

            // ITERATES THROUGH EVERY NODE IN THE NodeList
            for (int i = 0; i < shortDescriptionNodeList.getLength(); i++) {

                Node shortDescriptionNode = shortDescriptionNodeList.item(i);

                if (shortDescriptionNode.getTextContent().equals(shortDescription)) {

                    Node taskNode = shortDescriptionNode.getParentNode();
                    rootElement.removeChild(taskNode);

                    //UPDATES THE OBSERVABLE LIST THAT PROVIDES THE FXML ListView
                    taskItems.remove(taskItem);
                }
            }

            // WRITES THE CONTENT INTO XML FILE
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(inputFile);
            transformer.transform(source, result);

        } catch (Exception e) {
            System.out.println("Couldn't delete task items from XML file:  " + e.getMessage());
        }

    }

//    DELETE TASK ITEM FROM DATABASE
    private void deleteDBTaskItem(TaskItem taskItem) {

        String shortDescription = taskItem.getShortDescription();

        String deleteTaskStatement = "DELETE FROM taskitems WHERE shortDescription=\"" + shortDescription + "\"";

        try(Connection connection = DriverManager.getConnection(DB_CONNECTION_STRING);
            PreparedStatement deleteTaskitems = connection.prepareStatement(deleteTaskStatement)) {

            // EXECUTES DELETE FROM taskitems TABLE
            deleteTaskitems.execute();
            //UPDATES THE OBSERVABLE LIST THAT PROVIDES THE FXML ListView
            taskItems.remove(taskItem);

        } catch (SQLException e) {
            System.out.println("Couldn't open connection: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void editTaskItem(TaskItem oldItem, TaskItem newItem) {

        switch(this.dataSource) {
            case DB:
                editDBTaskItem(oldItem, newItem);
                System.out.println("Task item is edited in the SQLite database");
                break;

            case XML:
                editXMLTaskItem(oldItem, newItem);
                System.out.println("Task items is edited in the XLM file");
                break;
        }
    }

    private void editXMLTaskItem(TaskItem oldItem, TaskItem newItem) {

        try {
            // CREATES A DocumentBuilder FROM THE XML FILE
            File inputFile = new File(XML_CONNECTION_STRING);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            // CREATES A Document FROM A DocumentBuilder
            Document document = dBuilder.parse(inputFile);

            String oldShortDescription = oldItem.getShortDescription();
            String shortDescription = newItem.getShortDescription();
            String details = newItem.getDetails();
            String deadline = newItem.getDeadline().format(formatter);

            // RETURNS THE ROOT ELEMENT OF THE DOCUMENT
            Element rootElement = document.getDocumentElement();

            // CREATE A NODE LIST OF ELEMENTS BY TAG NAME
            NodeList shortDescriptionNodeList = document.getElementsByTagName("shortDescription");

            // ITERATES THROUGH EVERY shortDescription NODE IN THE NodeList
            for (int i = 0; i < shortDescriptionNodeList.getLength(); i++) {

                Node shortDescriptionNode = shortDescriptionNodeList.item(i);

                if (shortDescriptionNode.getTextContent().equals(oldShortDescription)) {

                    Node shortDescriptionParentNode = shortDescriptionNode.getParentNode();

                    NodeList childNodesList = shortDescriptionParentNode.getChildNodes();

                    for (int j = 0; j < childNodesList.getLength(); j++) {

                        Node childNode = childNodesList.item(j);

                        if (childNode.getNodeType() == Node.ELEMENT_NODE) {

                            Element editTaskElement = (Element) childNode;
                            String nodeName = editTaskElement.getNodeName();

                            switch(nodeName) {
                                case "shortDescription" :
                                    editTaskElement.setTextContent(shortDescription);
                                    break;
                                case "details" :
                                    editTaskElement.setTextContent(details);
                                    break;
                                case "deadline" :
                                    editTaskElement.setTextContent(deadline);
                                    break;
                            }
                        }
                    }

                    //UPDATES THE OBSERVABLE LIST THAT PROVIDES THE FXML ListView
                    int index = taskItems.indexOf(oldItem);
                    taskItems.set(index,newItem);
                }

            }

            // WRITES THE CONTENT INTO XML FILE
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(inputFile);
            transformer.transform(source, result);

        } catch (Exception e) {
            System.out.println("Couldn't edit task items in XML file:  " + e.getMessage());
            e.printStackTrace();
        }

    }

//    EDIT TASK ITEM IN DATABASE
    private void editDBTaskItem(TaskItem oldItem, TaskItem newItem) {

        String shortDescription = newItem.getShortDescription();
        String details = newItem.getDetails();
        String deadline = newItem.getDeadline().format(formatter);

        String oldShortDescription = oldItem.getShortDescription();

        String editTaskStatement = "UPDATE taskitems SET shortDescription=\"" + shortDescription +
                                                     "\", details=\"" + details +
                                                     "\", deadline=\"" + deadline +
                                                "\" WHERE shortDescription=\"" + oldShortDescription + "\"";

        try(Connection connection = DriverManager.getConnection(DB_CONNECTION_STRING);
            PreparedStatement editTaskitems = connection.prepareStatement(editTaskStatement)) {

            // EXECUTES UPDATE taskitems TABLE
            editTaskitems.execute();
            //UPDATES THE OBSERVABLE LIST THAT PROVIDES THE FXML ListView
            int index = taskItems.indexOf(oldItem);
            taskItems.set(index,newItem);

        } catch (SQLException e) {
            System.out.println("Couldn't open connection: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
