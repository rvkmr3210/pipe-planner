package edu.uark.pipeplanparser.model;

import android.support.annotation.NonNull;

import com.google.android.gms.maps.GoogleMap;

public interface Mappable {

    void addToMap(@NonNull GoogleMap googleMap);

    void removeFromMap();
}
