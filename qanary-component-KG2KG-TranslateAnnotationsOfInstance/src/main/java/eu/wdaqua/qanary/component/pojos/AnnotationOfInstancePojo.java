package eu.wdaqua.qanary.component.pojos;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.jena.rdf.model.RDFNode;

import java.util.List;

public class AnnotationOfInstancePojo {

    @JsonProperty("annotationId")
    private String annotationId;
    private String targetQuestion;
    private int start;
    private int end;
    private double score;
    private String originResource;
    private List<RDFNode> newResources;

    public AnnotationOfInstancePojo(String annotationId, String originResource, String targetQuestion, int start, int end, double score) {
        this.annotationId = annotationId;
        this.originResource = originResource;
        this.targetQuestion = targetQuestion;
        this.start = start;
        this.end = end;
        this.score = score;
    }

    public String getAnnotationId() {
        return annotationId;
    }

    public void setAnnotationId(String annotationId) {
        this.annotationId = annotationId;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getTargetQuestion() {
        return targetQuestion;
    }

    public void setTargetQuestion(String targetQuestion) {
        this.targetQuestion = targetQuestion;
    }

    public List<RDFNode> getNewResources() {
        return newResources;
    }

    public void setNewResources(List<RDFNode> newResources) {
        this.newResources = newResources;
    }

    public String getOriginResource() {
        return originResource;
    }

    public void setOriginResource(String originResource) {
        this.originResource = originResource;
    }

    @Override
    public String toString() {
        return "AnnotationOfInstancePojo{" + // 
                "targetQuestion='" + targetQuestion + '\'' + // 
                ", start=" + start + // 
                ", end=" + end + // 
                ", score=" + score + // 
                ", originResource='" + originResource + '\'' + // 
                ", newResource='" + newResources.toString() + '\'' + // 
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        AnnotationOfInstancePojo annotationOfInstancePojo = (AnnotationOfInstancePojo) obj;

        return this.annotationId == annotationOfInstancePojo.getAnnotationId();
    }
}
