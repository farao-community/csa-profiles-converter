import com.farao_community.farao.swe_csa.app.CsaProfilesConverterService;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.api.parameters.JsonCracCreationParameters;
import org.junit.jupiter.api.Test;

public class ConverterTest {

    @Test
    void test() {
        CracCreationParameters importedParameters = JsonCracCreationParameters.read(CsaProfilesConverterService.class.getResourceAsStream("/csa-crac-parameters.json"));

    }
}

