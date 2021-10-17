# Gogo Account

## Deployment

### Creating a new version

1. Update version number in `pom.xml` and `banner.txt`.
2. Use `splash.xsf` and GIMP to generate a new version of `splash.png` with the new version or year of copyright.
3. Build the jar file with Maven.

### Configuration emails

If you ever get an error message like "PKIX path building failed" or
"unable to find valid certification path to requested target" when sending
emails, then the certificate of the mail server is not known by your Java
runtime.

You can fix it by following these steps, which I found on
[this StackOverflow post](https://stackoverflow.com/questions/21076179/pkix-path-building-failed-and-unable-to-find-valid-certification-path-to-requ):

1. Go to URL in your browser:
   * firefox - click on HTTPS certificate chain (the lock icon right next to URL address). Click "more info" > "security" > "show certificate" > "details" > "export..". Pickup the name and choose file type example.cer
   * chrome - click on site icon left to address in address bar, select "Certificate" -> "Details" -> "Export" and save in format "Der-encoded binary, single certificate".
2. Now you have file with keystore and you have to add it to your JVM. Determine location of cacerts files, eg. C:\Program Files (x86)\Java\jre1.6.0_22\lib\security\cacerts.
3. Next import the example.cer file into cacerts in command line (may need administrator command prompt):


    cd 'C:\Program Files\java\jdk1.8.0_45\jre\bin'
    .\keytool -import -alias example -keystore  "c:\Program Files\Java\jdk1.8.0_45\jre\lib\security\cacerts" -file example.cer

You will be asked for password which default is `changeit`.

Restart your JVM.

To test the mail configuration you can run the test
`EmailServiceTest`. First remove the ignore 