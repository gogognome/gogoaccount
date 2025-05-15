# Gogo Account

## Introduction

Gogo Account is a versatile bookkeeping application designed for small organizations, sports clubs,
or even personal financial management.

Key features of Gogo Account include:

* Customizable accounts, allowing you to define your own accounts rather than being restricted to predefined ones.
* Journal functionality for recording transactions.
* Balance sheets and income statements for financial overview.
* Management of parties, which can be used as debtors or creditors or to manage the members of your sport club or employees of your organization.
* Invoice handling capabilities.

Gogo Account is licensed under [Apache License, version 2.0](https://www.apache.org/licenses/LICENSE-2.0).

## Download

You can download a prebuilt version of Gogo Account from [my website](https://gogognome.nl/gogo-account.html). Examples of templates for sending emails with invoices can be downloaded
from that page too.

## Start

Start Gogo Account from a command prompt or terminal shell using the following command:

    java -jar gogoaccount-3.0.14.jar [--lang=language code] [bookkeeping.h2.db]

The options between straight brackets `[` and `]` are optional. They may be omitted.

Replace `language code` by a language code, for example `en` for English or `nl` for Dutch.
If you omit this option then the program starts in English.

Replace `bookkeeping.h2.db` by the file name of the bookkeeping that must be opened when Gogo Account starts. If you omit
this option you have to open a bookkeeping via the user interface of Gogo Account.

For example, to start Gogo Account in English and without opening a bookkeeping use this command:

    java -jar gogoaccount-3.0.14.jar --lang=en

## Shell script for Linux

Linux users can easily create a shell script to start Gogo Account in English:

    echo java -jar gogoaccount-3.0.14.jar -lang=en > gogoaccount.sh

Make the shells script gogoaccount.sh executable:

    chmod a+x gogoaccount.sh

Now you can start Gogo Account using this shell script:

    ./gogoaccount.sh

## Releasing a new version

In case you want to release a new version of Gogo Account, follow these steps:

1. Update version number in `pom.xml` and `banner.txt`.
2. Update the year in copyright notices in `pom.xml` (for splash screen), `banner.txt`, `stringresource.properties` and `stringresources_nl.properties`.
3. Build the jar file with Maven.

The splash image and about image are generated in the compile step by the
`image-modifier-maven-plugin`.

## Troubleshooting

### Configuration emails

If you ever get an error message like "PKIX path building failed" or
"unable to find valid certification path to requested target" when sending
emails, then the certificate of the mail server is not known by your Java
runtime.

You can fix it by following these steps, which I found on
[this StackOverflow post](https://stackoverflow.com/questions/21076179/pkix-path-building-failed-and-unable-to-find-valid-certification-path-to-requ):

1. Go to URL in your browser:
   * Firefox - click on HTTPS certificate chain (the lock icon right next to URL address). Click "more info" > "security" > "show certificate" > "details" > "export..". Pickup the name and choose file type `example.cer`
   * Chrome - click on site icon left to address in address bar, select "Certificate" -> "Details" -> "Export" and save in format "Der-encoded binary, single certificate".
2. Now you have file with keystore and you have to add it to your JVM. Determine location of `cacerts` files, for example: `C:\Program Files (x86)\Java\jre1.6.0_22\lib\security\cacerts`
3. Next import the example.cer file into `cacerts` in command line (may need administrator command prompt):

    cd 'C:\Program Files\java\jdk1.8.0_45\jre\bin'
    .\keytool -import -alias example -keystore  "c:\Program Files\Java\jdk1.8.0_45\jre\lib\security\cacerts" -file example.cer

You will be asked for password which default is `changeit`.

Restart your JVM.

To test the mail configuration you can run the test
`EmailServiceTest` after you have removed the `@Disabled` annotation. 