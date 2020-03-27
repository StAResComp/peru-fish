[![DOI](https://zenodo.org/badge/153292227.svg)](https://zenodo.org/badge/latestdoi/153292227)

# Peruvian Fishing App

An Android app for Peruvian fishers based on that [developed](https://github.com/StAResComp/sifids) as part of the Scottish Inshore Fisheries Integrated Data System (SIFIDS) Project.

## Building from source

This app requires a [Google Maps API key](https://developers.google.com/maps/documentation/android-sdk/signup), and expects to find one in the system resources. Add a file `keys.xml` in `catch/src/main/res/xml` with the following content:

```XML
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="google_maps_api_key">your-api-key-goes-here</string>
</resources>
```
