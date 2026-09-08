package qz.common;

import com.github.zafarkhaja.semver.Version;
import qz.utils.SystemUtilities;

import java.awt.*;

import static qz.ws.SingleInstanceChecker.STEAL_WEBSOCKET_PROPERTY;

/**
 * Created by robert on 7/9/2014.
 *
 * Rebranded strings for the Labelific Print fork live here. The Java
 * package stays `qz.*` on purpose — renaming it would make every
 * upstream merge from qzind/tray a fight. All user-visible identity is
 * driven by the ABOUT_* + PROBE_* + PROPS_FILE + DATA_DIR constants
 * below, plus the strings in ant/project.properties and the platform
 * installer templates under ant/{apple,linux,windows}/.
 */
public class Constants {
    public static final String HEXES = "0123456789ABCDEF";
    public static final char[] HEXES_ARRAY = HEXES.toCharArray();
    public static final int BYTE_BUFFER_SIZE = 8192;
    public static final Version VERSION = Version.valueOf("2.2.6");
    public static final Version JAVA_VERSION = SystemUtilities.getJavaVersion();
    public static final String JAVA_VENDOR = System.getProperty("java.vendor");

    /* Labelific Print constants (rebrand of QZ Tray, LGPL-2.1) */
    public static final String BLOCK_FILE = "blocked";
    public static final String ALLOW_FILE = "allowed";
    public static final String TEMP_FILE = "temp";
    public static final String LOG_FILE = "debug";
    public static final String PROPS_FILE = "labelific-print"; // .properties extension is assumed
    public static final String PREFS_FILE = "prefs"; // .properties extension is assumed
    public static final String[] PERSIST_PROPS = {"file.whitelist", "file.allow", "networking.hostname", "networking.port", STEAL_WEBSOCKET_PROPERTY };
    public static final String AUTOSTART_FILE = ".autostart";
    public static final String DATA_DIR = "labelific";

    public static final int BORDER_PADDING = 10;

    public static final String ABOUT_TITLE = "Labelific Print";
    public static final String ABOUT_EMAIL = "support@labelific.com";
    public static final String ABOUT_URL = "https://labelific.com";
    public static final String ABOUT_COMPANY = "Labelific";
    public static final String ABOUT_CITY = "Prague";
    public static final String ABOUT_STATE = "";
    public static final String ABOUT_COUNTRY = "CZ";

    public static final String ABOUT_LICENSING_URL = Constants.ABOUT_URL + "/licensing";
    public static final String ABOUT_SUPPORT_URL = Constants.ABOUT_URL + "/support";
    public static final String ABOUT_PRIVACY_URL = Constants.ABOUT_URL + "/privacy";
    public static final String ABOUT_DOWNLOAD_URL = Constants.ABOUT_URL + "/download";

    // Auto-update source. GitHub Releases API for the fork today; will be
    // repointed at the Cloudflare R2 versions.json feed once that lands
    // (see docs/AUTO-UPDATE.md).
    public static final String VERSION_CHECK_URL = "https://api.github.com/repos/labelific/labelific-print/releases";
    public static final String VERSION_DOWNLOAD_URL = "https://github.com/labelific/labelific-print/releases";
    public static final boolean ENABLE_DIAGNOSTICS = true; // Diagnostics menu (logs, etc)

    public static final String BRAND_COLOR_HEX = "#0d6efd";
    // IS_REBRANDED flips true because ABOUT_EMAIL no longer matches QZ's.
    // Downstream code branches on it to hide QZ-specific menu items,
    // sponsorship text, and similar. Do NOT invert this by hand — just
    // change ABOUT_EMAIL and the flag follows.
    @SuppressWarnings("ConstantValue")
    public static final boolean IS_REBRANDED = !ABOUT_EMAIL.equals("support@qz.io");

    public static final String TRUSTED_CERT = String.format("Verified by %s", Constants.ABOUT_COMPANY);
    public static final String SPONSORED_CERT = String.format("Sponsored by %s", Constants.ABOUT_COMPANY);
    public static final String SPONSORED_TOOLTIP = "Sponsored organization";
    public static final String STRICT_MODE_CERT = "Strictly permitted";
    public static final String THIRD_PARTY_CERT = "Third-party issued";
    public static final String UNTRUSTED_CERT = "Untrusted website";
    public static final String NO_TRUST = "Cannot verify trust";

    public static final String PROBE_REQUEST = "getProgramName";
    public static final String PROBE_RESPONSE = ABOUT_TITLE;

    public static final String ALLOW_SITES_TEXT = "Permanently allowed \"%s\" to access local resources";
    public static final String BLOCK_SITES_TEXT = "Permanently blocked \"%s\" from accessing local resources";

    public static final String REMEMBER_THIS_DECISION = "Remember this decision";
    public static final String STRICT_MODE_LABEL = "Use strict certificate mode";
    public static final String STRICT_MODE_TOOLTIP = String.format("Prevents the ability to select \"%s\" for most websites", REMEMBER_THIS_DECISION);
    public static final String STRICT_MODE_CONFIRM = String.format("Set strict certificate mode?  Most websites will stop working with %s.", ABOUT_TITLE);
    public static final String ALLOW_SITES_LABEL = "Sites permanently allowed access";
    public static final String BLOCK_SITES_LABEL = "Sites permanently blocked from access";


    public static final String ALLOWED = "Allowed";
    public static final String BLOCKED = "Blocked";

    public static final String OVERRIDE_CERT = "override.crt";
    public static final String WHITELIST_CERT_DIR = "whitelist";
    public static final String PROVISION_DIR = "provision";
    public static final String PROVISION_FILE = "provision.json";

    public static final String SIGNING_PRIVATE_KEY = "private-key.pem";
    public static final String SIGNING_CERTIFICATE = "digital-certificate.txt";

    public static final long VALID_SIGNING_PERIOD = 15 * 60 * 1000; //millis
    public static final int EXPIRY_WARN = 30;   // days
    public static final Color WARNING_COLOR_LITE = Color.RED;
    public static final Color TRUSTED_COLOR_LITE = Color.BLUE;
    public static final Color WARNING_COLOR_DARK = Color.decode("#EB6261");
    public static final Color TRUSTED_COLOR_DARK = Color.decode("#589DF6");
    public static Color WARNING_COLOR = WARNING_COLOR_LITE;
    public static Color TRUSTED_COLOR = TRUSTED_COLOR_LITE;

    public static boolean MASK_TRAY_SUPPORTED = true;

    public static final long MEMORY_PER_PRINT = 512; //MB

    public static final String RAW_PRINT = ABOUT_TITLE + " Raw Print";
    public static final String IMAGE_PRINT = ABOUT_TITLE + " Pixel Print";
    public static final String PDF_PRINT = ABOUT_TITLE + " PDF Print";
    public static final String HTML_PRINT = ABOUT_TITLE + " HTML Print";

    public static final Integer[] DEFAULT_WSS_PORTS = {8181, 8282, 8383, 8484};
    public static final Integer[] DEFAULT_WS_PORTS = {8182, 8283, 8384, 8485};
    public static final Integer[] CUPS_RSS_PORTS = {8586, 8687, 8788, 8889};
}
