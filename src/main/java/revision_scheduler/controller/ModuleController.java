package revision_scheduler.controller;

import revision_scheduler.model.Module;
import revision_scheduler.repository.ModuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/modules")
@CrossOrigin(origins = "*")
public class ModuleController {

    @Autowired
    private ModuleRepository moduleRepository;

    @GetMapping
    public List<Module> getAllModules() {
        return moduleRepository.findAll();
    }

    @PostMapping
    public Module createModule(@RequestBody Module module) {
        return moduleRepository.save(module);
    }

    @DeleteMapping("/{id}")
    public void deleteModule(@PathVariable Long id) {
        moduleRepository.deleteById(id);
    }
}