package ru.ifmo.ctddev.solutions.crawler;

import info.kgeorgiy.java.advanced.crawler.CachingDownloader;
import info.kgeorgiy.java.advanced.crawler.Crawler;
import info.kgeorgiy.java.advanced.crawler.Document;
import info.kgeorgiy.java.advanced.crawler.Downloader;
import info.kgeorgiy.java.advanced.crawler.Result;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;



public class WebCrawler implements Crawler {

    AddMethod I = new AddMethod();

    public WebCrawler(Downloader Dwnldr, int Dwlns, int Extrs, int PHost) throws IOException {
        this.I.Dwnldr = Dwnldr;
        I.ExtrLock = new Semaphore(Extrs);
        I.DwnlLock = new Semaphore(Dwlns);
        if (Dwlns > I.DwnlLim)
            Dwlns = I.DwnlLim;
        if (Extrs > I.ExtrLim)
            Extrs = I.ExtrLim;
        I.MaxHosts = PHost;
        I.Extr = Executors.newWorkStealingPool(Extrs + Dwlns);
    }
    @Override
    public Result download(String PageU, int i) {
        if (i <= 0 || PageU == null) {
            return I.Res;
        }
        String host = I.GetHost(PageU);
        Document Doc = I.DwldPage(PageU, host);
        I.ExtrDoc(Doc, PageU);
        List<String> Dwld = new ArrayList<>();
        HashMap<String, IOException> Err = new HashMap<>();
        Dwld.addAll(I.TPages.get(PageU).getDownloaded());
        Err.putAll(I.TPages.get(PageU).getErrors());
        I.Num.getOrDefault(PageU, new HashSet<>()).stream().map(url -> (Callable<Result>) () -> download(url, i - 1)).map(I.Extr::submit).collect(Collectors.toList()).forEach(subResult ->
        {
            try {
                Result res = subResult.get();
                Dwld.addAll(res.getDownloaded());
                Err.putAll(res.getErrors());
            } catch (InterruptedException ie)
            {
                ie.printStackTrace();
            } catch (ExecutionException ee) {
                ee.printStackTrace();
            }
        });
        return new Result(Dwld, Err);
    }
    @Override
    public void close() {
        try
        {
            I.Extr.shutdownNow();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
