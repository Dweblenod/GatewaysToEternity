package shadows.gateways;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.serialization.Codec;

import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.RegistryEvent.Register;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import shadows.gateways.client.GatewayParticleData;
import shadows.gateways.client.GatewayTickableSound;
import shadows.gateways.command.GatewayCommand;
import shadows.gateways.entity.GatewayEntity;
import shadows.gateways.gate.Failure;
import shadows.gateways.gate.GatewayManager;
import shadows.gateways.gate.Reward;
import shadows.gateways.gate.WaveEntity;
import shadows.gateways.item.GatePearlItem;
import shadows.gateways.net.ParticleMessage;
import shadows.gateways.recipe.GatewayRecipeSerializer;
import shadows.placebo.network.MessageHelper;

@Mod(Gateways.MODID)
public class Gateways {

	public static final String MODID = "gateways";
	public static final Logger LOGGER = LogManager.getLogger("Gateways to Eternity");
	//Formatter::off
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(MODID, "channel"))
            .clientAcceptedVersions(s->true)
            .serverAcceptedVersions(s->true)
            .networkProtocolVersion(() -> "1.0.0")
            .simpleChannel();
    //Formatter::on
	public static final CreativeModeTab TAB = new CreativeModeTab(MODID) {

		@Override
		public ItemStack makeIcon() {
			return new ItemStack(GatewayObjects.GATE_PEARL);
		}

	};

	public Gateways() {
		FMLJavaModLoadingContext.get().getModEventBus().register(this);
		MessageHelper.registerMessage(CHANNEL, 0, new ParticleMessage());
		MinecraftForge.EVENT_BUS.addListener(this::commands);
	}

	@SubscribeEvent
	public void setup(FMLCommonSetupEvent e) {
		GatewayManager.INSTANCE.registerToBus();
		Reward.initSerializers();
		Failure.initSerializers();
		WaveEntity.initSerializers();
	}

	@SubscribeEvent
	public void registerEntities(Register<EntityType<?>> e) {
		//Formatter::off
		e.getRegistry().register(EntityType.Builder
				.<GatewayEntity>of(GatewayEntity::new, MobCategory.MISC)
				.setTrackingRange(5)
				.setUpdateInterval(20)
				.sized(2F, 3F)
				.setCustomClientFactory((se, w) -> {
					GatewayEntity ent = new GatewayEntity(GatewayObjects.GATEWAY, w);
					GatewayTickableSound.startGatewaySound(ent);
					return ent;
				})
				.build("gateway")
				.setRegistryName("gateway"));
		//Formatter::on
	}

	@SubscribeEvent
	public void registerItems(Register<Item> e) {
		e.getRegistry().register(new GatePearlItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).tab(TAB)).setRegistryName("gate_pearl"));
		registerStat(GatewayObjects.Stats.STAT_GATES_DEFEATED, StatFormatter.DEFAULT);
	}

	@SubscribeEvent
	public void registerSerializers(Register<RecipeSerializer<?>> e) {
		e.getRegistry().register(GatewayRecipeSerializer.INSTANCE.setRegistryName("gate_recipe"));
	}

	@SubscribeEvent
	public void registerSounds(Register<SoundEvent> e) {
		//Formatter::off
		e.getRegistry().registerAll(
				new SoundEvent(new ResourceLocation(MODID, "gate_warp")).setRegistryName("gate_warp"),
				new SoundEvent(new ResourceLocation(MODID, "gate_ambient")).setRegistryName("gate_ambient"),
				new SoundEvent(new ResourceLocation(MODID, "gate_start")).setRegistryName("gate_start"),
				new SoundEvent(new ResourceLocation(MODID, "gate_end")).setRegistryName("gate_end")
		);
		//Formatter::on
	}

	@SubscribeEvent
	public void registerParticles(Register<ParticleType<?>> e) {
		e.getRegistry().register(new ParticleType<GatewayParticleData>(false, GatewayParticleData.DESERIALIZER) {
			@Override
			public Codec<GatewayParticleData> codec() {
				return GatewayParticleData.CODEC;
			}
		}.setRegistryName("glow"));
	}

	public void commands(RegisterCommandsEvent e) {
		GatewayCommand.register(e.getDispatcher());
	}

	private static void registerStat(ResourceLocation id, StatFormatter pFormatter) {
		Registry.register(Registry.CUSTOM_STAT, id, id);
		Stats.CUSTOM.get(id, pFormatter);
	}

}
