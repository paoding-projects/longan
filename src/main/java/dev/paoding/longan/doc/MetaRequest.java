package dev.paoding.longan.doc;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MetaRequest {
    private List<MetaParam> params = new ArrayList<>();
    private Object sample;
    private List<MetaValidator> validators = new ArrayList<>();

    public void addMetaValidator(MetaValidator metaValidator){
        validators.add(metaValidator);
    }

    public void addMetaParam(MetaParam metaParam){
        params.add(metaParam);
    }
}
