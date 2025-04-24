package com.example.springaidifymcp.model;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class DifyWorkflow {
    private App app;
    private String kind;
    private String version;
    private Workflow workflow;
    
    @Data
    public static class App {
        private String description;
        private String icon;
        private String iconBackground;
        private String mode;
        private String name;
        private boolean useIconAsAnswerIcon;
    }
    
    @Data
    public static class Workflow {
        private List<Object> conversationVariables;
        private List<Object> environmentVariables;
        private Features features;
        private Graph graph;
    }
    
    @Data
    public static class Features {
        private FileUpload fileUpload;
        private String openingStatement;
        private RetrieverResource retrieverResource;
        private SensitiveWordAvoidance sensitiveWordAvoidance;
        private SpeechToText speechToText;
        private List<Object> suggestedQuestions;
        private SuggestedQuestionsAfterAnswer suggestedQuestionsAfterAnswer;
        private TextToSpeech textToSpeech;
        
        @Data
        public static class FileUpload {
            private List<String> allowedFileExtensions;
            private List<String> allowedFileTypes;
            private List<String> allowedFileUploadMethods;
            private boolean enabled;
            private FileUploadConfig fileUploadConfig;
            private Image image;
            private int numberLimits;
            
            @Data
            public static class FileUploadConfig {
                private int audioFileSizeLimit;
                private int batchCountLimit;
                private int fileSizeLimit;
                private int imageFileSizeLimit;
                private int videoFileSizeLimit;
                private int workflowFileUploadLimit;
            }
            
            @Data
            public static class Image {
                private boolean enabled;
                private int numberLimits;
                private List<String> transferMethods;
            }
        }
        
        @Data
        public static class RetrieverResource {
            private boolean enabled;
        }
        
        @Data
        public static class SensitiveWordAvoidance {
            private boolean enabled;
        }
        
        @Data
        public static class SpeechToText {
            private boolean enabled;
        }
        
        @Data
        public static class SuggestedQuestionsAfterAnswer {
            private boolean enabled;
        }
        
        @Data
        public static class TextToSpeech {
            private boolean enabled;
            private String language;
            private String voice;
        }
    }
    
    @Data
    public static class Graph {
        private List<Edge> edges;
        private List<Node> nodes;
        private Viewport viewport;
        
        @Data
        public static class Edge {
            private EdgeData data;
            private String id;
            private String source;
            private String sourceHandle;
            private String target;
            private String targetHandle;
            private String type;
            
            @Data
            public static class EdgeData {
                private String sourceType;
                private String targetType;
            }
        }
        
        @Data
        public static class Node {
            private NodeData data;
            private boolean dragging;
            private int height;
            private String id;
            private Position position;
            private Position positionAbsolute;
            private boolean selected;
            private String sourcePosition;
            private String targetPosition;
            private String type;
            private int width;
            
            @Data
            public static class NodeData {
                private String answer;
                private List<NodeClass> classes;
                private String desc;
                private Context context;
                private List<String> datasetIds;
                private Map<String, Object> memory;
                private Map<String, Object> model;
                private List<Map<String, Object>> promptTemplate;
                private String queryVariableSelector;
                private String retrievalMode;
                private boolean selected;
                private Object singleRetrievalConfig;
                private String title;
                private List<String> topics;
                private String type;
                private List<Map<String, Object>> variables;
                private Map<String, Object> vision;
                
                @Data
                public static class NodeClass {
                    private String id;
                    private String name;
                }
                
                @Data
                public static class Context {
                    private boolean enabled;
                    private List<String> variableSelector;
                }
            }
            
            @Data
            public static class Position {
                private double x;
                private double y;
            }
        }
        
        @Data
        public static class Viewport {
            private int x;
            private double y;
            private double zoom;
        }
    }
}