package berlin.yuna.nano.services.ocr;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static berlin.yuna.nano.core.model.NanoThread.VIRTUAL_THREAD_POOL;
import static java.lang.System.lineSeparator;
import static java.util.Arrays.stream;

public class Categorize {

    public static final Map<String, List<String>> CATEGORY_KEYWORDS;
    public static final String INPUT_FILE = "/Users/yuna/Downloads/ausweis.png";

    public static void main(final String[] args) {
        System.out.println(lineSeparator() + "########## TESSERACT ###########");
        System.out.println("PATH [" + OCRProcessor.TESSERACT_PATH + "]");
        System.out.println("VERSION [" + OCRProcessor.TESSERACT_VERSION + "]");
        System.out.println("LANGUAGES [" + OCRProcessor.TESSERACT_LANGUAGES + "]");

        System.out.println(lineSeparator() + "########## OCR ###########");
        long start = System.currentTimeMillis();
        final OCRProcessor.OCRMetadata metadata = OCRProcessor.parseText(Path.of(INPUT_FILE));
        System.out.println("Duration [" + (System.currentTimeMillis() - start) + "ms]");
        System.out.println("metadata = " + metadata);

        System.out.println(lineSeparator() + "########## SCORING ###########");
        start = System.currentTimeMillis();
        final Map<String, Double> stringDoubleMap = categoryScores(metadata.textBlocks);
        System.out.println("Duration [" + (System.currentTimeMillis() - start) + "ms]");
        System.out.println("scores = " + stringDoubleMap.entrySet().stream().sorted(Map.Entry.<String, Double>comparingByValue().reversed()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new)));
    }

    public static Map<String, Double> categoryScores(final List<OCRProcessor.TextBlock> textBlocks) {
        final Map<String, Double> result = new ConcurrentHashMap<>();
        final List<Callable<Object>> tasks = new ArrayList<>();

        CATEGORY_KEYWORDS.forEach((category, keywords) ->
            tasks.add(Executors.callable(() -> {
                for (final String keyword : keywords) {
                    textBlocks.forEach(block -> {
                        // POOR SCORING, can be improved a lot :D e.g. finding most matching keywords and better algorithm
                        final int distance = damerauLevenshteinDistance(block.text.toLowerCase(), keyword.toLowerCase());
                        final double score = 100.0 - ((double) distance / Math.max(block.text.length(), keyword.length()) * 100);
                        if("PERSONALAUSWEIS".equalsIgnoreCase(block.text.trim()) && "PERSONALAUSWEIS".equalsIgnoreCase(keyword.trim())){
                            System.out.println("distance = " + distance);
                            System.out.println("score = " + score);
                        }
                        if (score > 50)
                            result.merge(category, score, Double::max);
                    });
                }
            })));

        executeTasks(tasks);
        return result;
    }

    public static void executeTasks(final List<Callable<Object>> tasks) {
        try {
            List<Future<Object>> futures = VIRTUAL_THREAD_POOL.invokeAll(tasks);

            for (Future<Object> future : futures) {
                try {
                    future.get(); // This will throw an exception if an error occurred during the task.
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static int damerauLevenshteinDistance(final String wortOne, final String wordTwo) {
        final int INFINITY = wortOne.length() + wordTwo.length();
        final int[][] wordLengths = new int[wortOne.length() + 2][wordTwo.length() + 2];
        wordLengths[0][0] = INFINITY;
        for (int i = 0; i <= wortOne.length(); i++) {
            wordLengths[i + 1][1] = i;
            wordLengths[i + 1][0] = INFINITY;
        }
        for (int j = 0; j <= wordTwo.length(); j++) {
            wordLengths[1][j + 1] = j;
            wordLengths[0][j + 1] = INFINITY;
        }

        final Map<Character, Integer> alphabet = new HashMap<>();
        for (int i = 1; i <= wortOne.length(); i++) {
            int db = 0;
            for (int j = 1; j <= wordTwo.length(); j++) {
                final int i1 = alphabet.getOrDefault(wordTwo.charAt(j - 1), 0);
                final int j1 = db;
                final int d = (wortOne.charAt(i - 1) == wordTwo.charAt(j - 1)) ? 0 : 1;
                if (d == 0) db = j;

                wordLengths[i + 1][j + 1] = min(
                    wordLengths[i][j] + d,
                    wordLengths[i + 1][j] + 1,
                    wordLengths[i][j + 1] + 1,
                    wordLengths[i1][j1] + (i - i1 - 1) + 1 + (j - j1 - 1)
                );
            }
            alphabet.put(wortOne.charAt(i - 1), i);
        }
        return wordLengths[wortOne.length() + 1][wordTwo.length() + 1];
    }

    private static int min(final int... nums) {
        return stream(nums).min().orElse(Integer.MAX_VALUE);
    }

    static {
        final Map<String, List<String>> result = new HashMap<>();
        result.put("Abgeschlossenheit", List.of("Abgeschlossenheitsbescheinigung", "Aufteilungsplan", "Abgeschlossenheitsbestätigung", "Antrag auf Abgeschlossenheit"));
        result.put("Abloesevollmacht", List.of("Ablösevollmacht", "Kreditwechselservice", "KWS"));
        result.put("Abtretung", List.of("Abtretungsanzeige", "Bausparverträge", "Finanzierungsbestätigungen", "Gehaltsabtretungen", "KFZ-Brief", "Lebensversicherungen", "Mietforderungen", "Rückgewähransprüche"));
        result.put("Altersvorsorge", List.of("Private Altersvorsorge", "betriebliche Altersvorsorge", "Rentenversicherungen", "Betriebsrenten", "Pensionsfonds"));
        result.put("Anmeldebestaetigung", List.of("Anmeldebestätigung Meldeamt", "Einwohnermeldeamt", "Ersatzformulare", "Geburtsurkunden", "Sterbeurkunden", "Erbschein", "Erklärung zur Selbstnutzung"));
        result.put("Anschriftenaenderung", List.of("Formular Anschriftenänderung", "Ummeldung", "Einzug"));
        result.put("Ausweis", List.of("Ausweisdokument", "Reisepass", "Personalausweis", "Aufenthaltstitel", "Niederlassungserlaubnis"));
        result.put("BWA", List.of("BWA", "Bilanz", "GuV", "E/A-Rechnung", "EÜR", "Jahresabschluss", "Überschussrechnung", "Gewinn- und Verlustrechnung", "Handelsregisterauszug"));
        result.put("Bauantrag", List.of("Bauantrag", "Bauanzeige", "Baugenehmigung", "Baugesuch"));
        result.put("Baubeschreibung", List.of("Baubeschreibung", "Objektbeschreibung", "Leistungsbeschreibung"));
        result.put("Baugenehmigung", List.of("Baugenehmigung", "Baustellenschild", "Baubeginnanzeige", "Anzeige über Baufertigstellung", "Bauherrenerklärung", "Gebührenbescheid", "Genehmigungsfreistellung"));
        result.put("Baulasten", List.of("Baulastenverzeichnis", "Altlasten"));
        result.put("Bauplan", List.of("Bauplan", "Grundriss", "Bauzeichnung", "Schnittzeichnung"));
        result.put("Baurechtsauskunft", List.of("Baurechtliche Auskunft", "Wasserrecht", "B-Plan Änderung", "Baurechtsnachweis"));
        result.put("Bauspar_Jahreskontoauszug", List.of("Jahreskontoauszug Bausparvertrag", "Eigenkapitalnachweis"));
        result.put("Bausparantrag", List.of("Bausparantrag", "Bausparen", "Neuabschluss BSV"));
        result.put("Bausparvertrag", List.of("Bausparvertrag", "Zuteilungsschreiben Bausparvertrag", "Bausparurkunde"));
        result.put("Begleitschreiben", List.of("Begleitschreiben", "Schriftverkehr", "E-Mails", "Informationen an Produktanbieter"));
        result.put("Beratungsdokument", List.of("Beratungsdokumente für Vertrieb", "Checklisten", "Deckblätter und Inhaltsverzeichnisse"));
        result.put("Beratungsprotokoll", List.of("Beratungsprotokoll", "Ratenschutz", "Beratungsdokumentation", "Restschuldversicherung", "Restkreditversicherung", "RSV"));
        result.put("Berechnungen", List.of("Bauwerksberechnungen", "Wohnflächenberechnung", "Kubatur", "Umbauter Raum"));
        result.put("Besichtigungsauftrag", List.of("Objektbesichtigung", "Gutachtenauftrag", "Bewertungsauftrag"));
        result.put("Besichtigungsbericht", List.of("Besichtigungsbericht", "Besichtigungsprotokoll"));
        result.put("DVV_VVI", List.of("Darlehensvermittlungsvertrag", "Vorvertragliche Informationen", "§491a BGB"));
        result.put("Darlehensantrag", List.of("Immobiliendarlehens-Antrag", "Immobiliendarlehens-Vertrag", "Prolongation", "Zinsanpassung", "Konditionsänderung", "Baufinanzierungsantrag", "Bestehender Altvertrag", "Drittmittel"));
        result.put("ESM", List.of("Europäisches Standardisiertes Merkblatt", "ESIS"));
        result.put("Ehevertrag", List.of("Ehevertrag", "Gütertrennung", "Heiratssurkunde"));
        result.put("Einkommensteuer", List.of("Einkommensteuer", "Steuererklärung", "EkSt"));
        result.put("Einkommensteuerbescheid", List.of("Einkommensteuerbescheid", "EkSt Bescheid", "Lohnsteuerbescheid", "Steuerfestsetzung"));
        result.put("Elterngeldbescheid", List.of("Elterngeldbescheid", "Antrag auf Elterngeld", "Baukindergeld"));
        result.put("Empfangsbestaetigung", List.of("Empfangsbestätigung für ESIS", "DVV", "VVI", "Merkblätter"));
        result.put("Energieausweis", List.of("Energieausweis", "EnEV", "Gebäudepass"));
        result.put("Erbbaurechtsvertrag", List.of("Erbbaurechtsvertrag", "Erklärung zum Erbrecht"));
        result.put("Erlaeuterungsprotokoll", List.of("Erläuterungsprotokoll", "Erläuterungen zur Darlehensvermittlung"));
        result.put("Eroeffnung_Girokonto", List.of("Kontoeröffnung", "Girokonto", "Gehaltskonto"));
        result.put("Erschliessung", List.of("Erschließungsnachweis", "Erschließungsbeiträge", "Erschließungsbestätigung", "Anliegerbeiträge", "Wasser", "Gas", "Telekom"));
        result.put("Expose", List.of("Exposé", "Zusammenfassung", "Immobilienexposé"));
        result.put("Faelligkeitsmitteilung", List.of("Fälligkeitsmitteilung", "Notar"));
        result.put("Finanzierungsvorschlag", List.of("Finanzierungsvorschlag", "unverbindlicher Finanzierungsvorschlag", "Finanzierungsangebot"));
        result.put("Finanzierungsvorschlag_Antwort", List.of("Antwortschreiben zum Finanzierungsvorschlag"));
        result.put("Finanzierungszusage", List.of("Finanzierungszusage", "vorläufige Finanzierungszusage", "Drittmittel-Bestätigung", "Kreditbestätigung", "Förderzusage", "Genehmigungsschreiben", "Darlehenszusage", "Grundsatz...", "Kreditzusage", "Vollvalutierungsbestätigung"));
        result.put("Foerderdarlehen", List.of("Förderdarlehen", "Förderdarlehensantrag", "Förderdarlehenszusage", "Landesbanken", "Investitionsbanken", "Förderzusage"));
        result.put("Freistellungsvereinbarung", List.of("Freistellungsvereinbarung", "Freistellungserklärung", "Freigabeversprechen", "§3 MaBV", "Globalfreistellung"));
        result.put("Gebaeudeversicherung", List.of("Gebäudeversicherung", "Wohngebäudeversicherung", "Versicherungsschutz", "Rohbauversicherung", "Feuerversicherung", "Haftpflichtversicherung"));
        result.put("Gehaltsabrechnung", List.of("Lohnabrechnung", "Gehaltsabrechnung", "Bezügemitteilung", "Rentenabrechnung", "Sold", "Einkommensnachweis"));
        result.put("Grundbuchauszug", List.of("Grundbuchauszug", "Auskunftseinwilligung", "Eintragungsbekanntmachung", "Erbbaugrundbuch", "Mitteilungen vom Grundbuchamt"));
        result.put("Grunderwerbsteuer", List.of("Grunderwerbsteuer", "Grundsteuer", "Steuerbescheid", "Grundsteuerbescheid"));
        result.put("Grundschuldbestellung", List.of("Grundschuldbestellung", "Grundschuldbrief", "vollstreckbare Grundschuld", "Aufgebotsverfahren", "Löschungsbewilligung", "Pfandfreigabe", "Treuhandauftrag", "Schuldanerkenntnis", "Schuldversprechen"));
        result.put("Inkasso", List.of("Inkasso", "Mahnung", "Forderung", "Kündigung", "§489 BGB", "Vorfälligkeit", "Zwangsversteigerung"));
        result.put("Kaufvertrag", List.of("Notarieller Kaufvertrag", "Kaufvertragsurkunde", "Kaufvertragsentwurf", "Anlagen zum Kaufvertrag", "Tauschvertrag", "Schenkungsurkunde", "Übertragungsvertrag", "Kaufabsichtserklärung"));
        result.put("KfW_Antrag", List.of("KfW Antrag", "KfW Fördermittel", "Wohneigentumsförderung", "KfW Beiblatt zur Baufinanzierung", "Einwilligungserklärung"));
        result.put("KfW_Antragsbestaetigung", List.of("KfW Bestätigung zum Antrag", "KfW Onlinebestätigung"));
        result.put("KfW_Durchfuehrungsbestaetigung", List.of("KfW Bestätigung nach Durchführung", "Durchführungsbestätigung"));
        result.put("Kontoauszug", List.of("Kontoauszug", "Girokonto", "Kreditkarte", "Depot", "Portfolio", "Darlehen", "Finanzstatus", "Kreditkartenumsatz", "Schenkungen", "Eigenkapitalnachweis"));
        result.put("Kostenaufstellung", List.of("Kostenaufstellung", "Baukosten", "Modernisierungskosten", "Eigenleistungen", "Angebote", "Kostenvoranschlag", "Reservierungsvereinbarung"));
        result.put("Krankenversicherungsnachweis", List.of("Krankenversicherungsnachweis", "Privatversicherung", "Änderungsmitteilung", "Bescheinigung", "Versicherungsschein"));
        result.put("Leerseite", List.of("Leerseite", "Seite ohne Inhalt"));
        result.put("Legitimationspruefung", List.of("Legitimationsprüfung", "Identitätsprüfung"));
        result.put("Lohnsteuerbescheinigung", List.of("Lohnsteuerbescheinigung", "Elektronische Lohnsteuerbescheinigung"));
        result.put("Mietvertrag", List.of("Mietvertrag", "Vermietungsbestätigung", "Pacht"));
        result.put("Nachrangdarlehen", List.of("Privatdarlehen", "Privatdarlehensantrag", "Privatdarlehenszusage", "nachrangiges Darlehen", "Kreditbestätigung Nachrangdarlehen"));
        result.put("Objektfotos", List.of("Objektfotos", "Bilder", "Fotos (innen)", "Fotos (außen)", "Baufortschrittsfotos"));
        result.put("Plankarten", List.of("Flurkarte", "Lageplan", "Bebauungsplan", "Fortführungserklärung", "BORIS", "Bodenrichtwerte", "Liegeschaftskarte", "Katasterkarte"));
        result.put("Privatkreditvertrag", List.of("Privatkredit", "Ratenkreditvertrag", "Ratenkreditantrag", "Neuabschluss und Ablösung von Krediten", "Abzulösende Konsumkredite", "Leasing", "Ratenschutz", "RSV"));
        result.put("Ratenschutzversicherung", List.of("Ratenschutzversicherung", "Restschuldversicherung", "Restkreditversicherung", "RSV"));
        result.put("Rechnung_Quittung", List.of("Rechnungen", "Verbrauchsgüterkaufvertrag", "Nebenkosten", "Betriebskosten", "Notarkosten", "Erschließungsbeiträge", "Maklergebühren", "Kaufvertrag für Konsumgüter", "Autoverkauf"));
        result.put("Rentenbescheid", List.of("Rentenbescheid", "Rentenanpassung", "Altersrente"));
        result.put("Renteninformation", List.of("Renteninformation", "zukünftige gesetzliche Altersrente"));
        result.put("Saldenmitteilung", List.of("Ablöseschreiben", "Saldenmitteilung", "Zinsbescheinigung", "Valutenbescheinigung", "Ablöseinformation"));
        result.put("Scheidungsbeschluss", List.of("Scheidungsbeschluss", "Scheidungsurteil"));
        result.put("Scheidungsfolgevereinbarung", List.of("Scheidungsfolgevereinbarung", "Notarielle Scheidungsfolgevereinbarung", "Trennungsvereinbarung"));
        result.put("Selbstauskunft", List.of("Selbstauskunft", "Schufa", "Erfassungsbogen", "Datenschutzklausel", "Einwilligung zu Auskünften", "Werbung", "Bankauskunft"));
        result.put("SEPA_Mandat", List.of("SEPA Lastschriftmandat"));
        result.put("Sicherungsvereinbarung", List.of("Sicherungsvereinbarung für Grundschuld", "Abtretung der Rückgewähransprüche"));
        result.put("Sonstige_Einnahmen", List.of("Sonstige Einnahmen", "Waisenrente", "Krankengeld", "Pflegegeld", "Einspeisevergütung"));
        result.put("Teilungserklaerung", List.of("Notarielle Teilungserklärung", "Pläne", "Verwaltervertrag", "Mieteraufstellung", "Eigentümerversammlung", "Wirtschaftsplan", "Neufassung", "Abschrift", "Vollmacht"));
        result.put("Uebergabeprotokoll", List.of("Übergabeprotokoll an Produktanbieter"));
        result.put("Ueberweisungsbeleg", List.of("Überweisungsbeleg", "Kontoumsatzdetails", "Einzahlungsbeleg", "Buchungsnachweis", "Ausdruck Online Banking"));
        result.put("Unterhaltsnachweis", List.of("Unterhaltsnachweis", "Beschluss", "Urkunde", "amtliches Schreiben", "Jugendamt", "persönliche Erklärung zum Unterhalt", "Kindergeldbescheid"));
        result.put("Unterlage_Arbeitgeber", List.of("Arbeitgeberunterlagen", "Arbeitsvertrag", "Bescheinigung Elternzeit", "Ernennungsurkunde", "Weiterbeschäftigung"));
        result.put("Vermittlerabfrage", List.of("Vermittlerabfrage"));
        result.put("Vermoegensuebersicht", List.of("Vermögensübersicht", "Vermögensaufstellung", "Immobilienaufstellung"));
        result.put("Werkvertrag", List.of("Werkvertrag", "Bauvertrag", "Bauwerkvertrag", "Architektenvertrag", "Bauträgervertrag", "Freistellung Steuerabzug §48 EStG"));
        result.put("Wertgutachten", List.of("Wertgutachten", "Vollgutachten", "Kurzgutachten", "Objektbewertung"));
        result.put("Wertindikation", List.of("Wertindikation", "Sprengnetter"));
        result.put("Zahlungsabruf", List.of("Zahlungsabruf", "Baufortschrittsanzeige", "Bautenstandsbericht", "Bauprotokoll", "Auszahlungsanweisung", "Verwendungsnachweis", "Erklärung zur Sofortigen Auszahlung"));
        result.put("Zahlungsplan", List.of("Zahlungsplan", "Zahlplan", "Teilzahlungen", "Auszahlungsplan"));
        result.put("Zustellungsvollmacht", List.of("Zustellungsvollmacht"));
        result.put("Zustimmungserklaerung", List.of("Zustimmungserklärung", "Darlehensaufnahme", "Besicherung", "Zustimmung des Ehepartners", "Objektwechsel", "Rangrücktritt", "Stillhalteerklärung"));
        CATEGORY_KEYWORDS = result;
    }
}
