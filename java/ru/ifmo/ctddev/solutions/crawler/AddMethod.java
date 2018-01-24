package com.com;

import info.kgeorgiy.java.advanced.crawler.Document;
import info.kgeorgiy.java.advanced.crawler.Downloader;
import info.kgeorgiy.java.advanced.crawler.Result;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static info.kgeorgiy.java.advanced.crawler.URLUtils.getHost;

public class AddMethod {
    public static Result Res = new Result(Collections.emptyList(), Collections.emptyMap());
    public static  int ExtrLim = 140;
    public static  int DwnlLim = 140;
    public Map<String, Lock> DwnldLocks = new ConcurrentHashMap<>();
    public  Map<String, Semaphore> HostC = new ConcurrentHashMap<>();
    public Downloader Dwnldr;
    public ExecutorService Extr;
    public  int MaxHosts;
    public  Map<String, Lock> ExtrLocks = new ConcurrentHashMap<>();
    public  Map<String, Result> TPages = new ConcurrentHashMap<>();
    public  Map<String, Set<String>> Num = new ConcurrentHashMap<>();
    public  Semaphore ExtrLock;
    public  Semaphore DwnlLock;

    public String GetHost(String PageU) {
        String host;
        try {
            host = getHost(PageU);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Can't get the host from url :" + PageU);
        }
        return host;
    }
    public Document DwldPage(String PageU, String host) {
        Document Doc = null;
        if (TPages.containsKey(PageU)==false) {
            DwnldLocks.putIfAbsent(PageU, new ReentrantLock());
            DwnldLocks.get(PageU).lock();
            try {
                if (!TPages.containsKey(PageU)) {
                    HostC.putIfAbsent(host, new Semaphore(MaxHosts));
                    try {
                        DwnlLock.acquire();
                        HostC.get(host).acquire();
                        try {
                            Doc = Dwnldr.download(PageU);
                            TPages.put(PageU, new Result(Collections.singletonList(PageU), new HashMap<>()));
                        } catch (IOException e) {
                            HashMap<String, IOException> Err = new HashMap<>();
                            Err.put(PageU, e);
                            TPages.put(PageU, new Result(new ArrayList<>(), Err));
                        }
                    } catch (InterruptedException e) {
                        throw new IllegalStateException("Can't acquire the semaphore");
                    } finally {
                        HostC.get(host).release();
                        DwnlLock.release();
                    }
                }
            } finally {
                DwnldLocks.get(PageU).unlock();
            }
        }

        return Doc;
    }
    public void ExtrDoc(Document Doc, String PageU) {
        if (Num.containsKey(PageU)==false)
        {
            ExtrLocks.putIfAbsent(PageU, new ReentrantLock());
            ExtrLocks.get(PageU).lock();
            try {
                if (Doc != null)
                    if (Num.containsKey(PageU)==false)
                    {
                        List<String> childUrls = new ArrayList<>();
                        try {
                            ExtrLock.acquire();
                            childUrls = Doc.extractLinks();
                        } catch (IOException io) {
                            io.printStackTrace();
                        } catch (InterruptedException e) {
                            throw new IllegalStateException("Unable to get semaphore");
                        } finally {
                            ExtrLock.release();
                        }
                        Num.put(PageU, new HashSet<>(childUrls));
                    }
            } finally
            {
                ExtrLocks.get(PageU).unlock();
            }
        }
    }
}
