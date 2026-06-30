package com.growgate.publicdata.service;

import com.growgate.publicdata.entity.PublicDataCollectionRun;
import com.growgate.publicdata.entity.PublicDataRecord;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PublicDataCollectionResult {

    private PublicDataCollectionRun run;
    private List<PublicDataRecord> records;
}
