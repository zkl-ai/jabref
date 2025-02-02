package org.jabref.logic.importer.fetcher;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.strings.StringUtil;
import org.jabref.model.util.OptionalUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetcher for ISBN trying ebook.de first, chimbori.com and then ottobib
 */
public class IsbnFetcher implements EntryBasedFetcher, IdBasedFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(IsbnFetcher.class);
    private static final Pattern NEWLINE_SPACE_PATTERN = Pattern.compile("\\n|\\r\\n|\\s");
    protected final ImportFormatPreferences importFormatPreferences;

    public IsbnFetcher(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
    }

    @Override
    public String getName() {
        return "ISBN";
    }

    @Override
    public Optional<HelpFile> getHelpPage() {
        return Optional.of(HelpFile.FETCHER_ISBN);
    }

    @Override
    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
        if (StringUtil.isBlank(identifier)) {
            return Optional.empty();
        }
        // remove any newlines and spaces.
        identifier = NEWLINE_SPACE_PATTERN.matcher(identifier).replaceAll("");

        OpenLibraryFetcher openLibraryFetcher = new OpenLibraryFetcher(importFormatPreferences);

        Optional<BibEntry> bibEntry = Optional.empty();
        try {
            bibEntry = openLibraryFetcher.performSearchById(identifier);
        } catch (FetcherException ex) {
            LOGGER.debug("Got a fetcher exception for IBSN search", ex);
        } finally {
            // nothing found at OpenLibrary: try ebook.de
            if (!bibEntry.isPresent()) {
                LOGGER.debug("No entry found at OpenLibrary; trying ebook.de");
                IsbnViaEbookDeFetcher isbnViaEbookDeFetcher = new IsbnViaEbookDeFetcher(importFormatPreferences);
                bibEntry = isbnViaEbookDeFetcher.performSearchById(identifier);
            }
        }

        return bibEntry;
    }

    @Override
    public List<BibEntry> performSearch(BibEntry entry) throws FetcherException {
        Optional<String> isbn = entry.getField(StandardField.ISBN);
        if (isbn.isPresent()) {
            return OptionalUtil.toList(performSearchById(isbn.get()));
        } else {
            return Collections.emptyList();
        }
    }
}
