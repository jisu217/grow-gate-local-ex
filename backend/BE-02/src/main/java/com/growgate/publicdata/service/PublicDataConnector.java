package com.growgate.publicdata.service;

import com.growgate.publicdata.enums.PublicDataProvider;

public interface PublicDataConnector {

    PublicDataProvider provider();

    String usedFor();

    String requestKey(PublicDataRequest request);

    PublicDataConnectorResult collect(PublicDataRequest request);
}
