package it.cnr.ilc.texto.controller;

import it.cnr.ilc.texto.manager.MonitorManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author oakgen
 */
@RestController
@RequestMapping("monitor")
public class MonitorController extends Controller {

    @GetMapping("{uuid}")
    public MonitorManager.Monitor get(@PathVariable("uuid") String uuid) throws Exception {
        return monitorManager.getMonitor(uuid);
    }

    @GetMapping("{uuid}/stop")
    public void stop(@PathVariable("uuid") String uuid) throws Exception {
        monitorManager.stopMonitor(uuid);
    }
}
